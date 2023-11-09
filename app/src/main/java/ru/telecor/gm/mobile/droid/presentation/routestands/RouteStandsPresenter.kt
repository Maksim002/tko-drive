package ru.telecor.gm.mobile.droid.presentation.routestands

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch
import moxy.InjectViewState
import ru.telecor.gm.mobile.droid.Screens
import ru.telecor.gm.mobile.droid.entities.RouteInfo
import ru.telecor.gm.mobile.droid.entities.StatusType
import ru.telecor.gm.mobile.droid.entities.VisitPoint
import ru.telecor.gm.mobile.droid.entities.stand.Stand
import ru.telecor.gm.mobile.droid.entities.dumping.GetListCouponsModel
import ru.telecor.gm.mobile.droid.entities.task.TaskRelations
import ru.telecor.gm.mobile.droid.model.data.storage.SettingsPrefs
import ru.telecor.gm.mobile.droid.model.interactors.RouteInteractor
import ru.telecor.gm.mobile.droid.model.mappers.ConverterMappers
import ru.telecor.gm.mobile.droid.model.system.IResourceManager
import ru.telecor.gm.mobile.droid.presentation.base.BasePresenter
import ru.telecor.gm.mobile.droid.utils.ConnectivityUtils
import ru.terrakok.cicerone.Router
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

/**
 * Project Truck Crew
 * Package ru.telecor.gm.mobile.droid.presentation.routestands
 *
 *
 *
 * Created by Emil Zamaldinov (aka piligrim) 17.07.2020
 * Copyright © 2020 TKO-Inform. All rights reserved.
 */
@InjectViewState
class RouteStandsPresenter @Inject constructor(
    private val routeInteractor: RouteInteractor,
    private val router: Router,
    private val rm: IResourceManager,
    private val converterMappers: ConverterMappers,
    private val settingsPrefs: SettingsPrefs
) : BasePresenter<RouteStandsView>() {

    var isNearFilterOn = false
    lateinit var localNearStandsList: List<Stand>
    private var query: String? = null
    var isEdit = false
    var taskId: Int = 0

    var errorFlightTicketModel = MutableLiveData<String>()
    var getListCouponsModel = arrayListOf(GetListCouponsModel())
    var listItem = ArrayList<GetListCouponsModel>()
    private var taskItemCon: List<TaskRelations> = arrayListOf()

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        getFlightTicketModel()
        updateTime()

        //event when task list is updated
        routeInteractor.addOnTasksUpdatedListener {
            launch {
                var tasks = routeInteractor.getTaskWithRelations()
                val currentTask = routeInteractor.getCurrentTask()
                handleResult(currentTask, { ct ->
                    tasks =
                        tasks.map { t -> t.copy(task = t.task.copy(isCurrent = t.task.id == ct.data.id)) }
                }, {})
                filterAndSetTasksList(tasks)
            }
        }
    }

    fun routes(): RouteInfo?{
        return routeInteractor.startedRoute
    }

    override fun attachView(view: RouteStandsView?) {
        super.attachView(view)
        isEdit = routeInteractor.isEdited
        taskId = routeInteractor.taskId

    }

    // Получение талонов
    private fun getFlightTicketModel() {

        viewState.showLoading(true)
        launch {
            val res = routeInteractor.getFlightTicketModel()
            loadAndSetTasksList(true)
            handleResult(res, { it ->
                listItem = it.data as ArrayList<GetListCouponsModel>
            }, {
                handleError(it, rm)
                errorFlightTicketModel.value = it.toString()
            })
        }
    }

    private fun loadAndSetTasksList(scrollToCurrent: Boolean = false) {
        launch {
            var refresh = false
            var tasks = routeInteractor.getTaskWithRelations()
            if (tasks.count() == tasks.filter { it.task.statusType == StatusType.NEW }.count()) {
                routeInteractor.getTaskProcessingResults()?.let { rP ->
                    if (rP.count() > 0 && ConnectivityUtils.syncAvailability(routeInteractor.getContext())) {
                        routeInteractor.startedRoute?.id?.let { rId ->
                                handleResult(
                                    routeInteractor.loadTasksForCurrentRoute(
                                        rId,
                                        true
                                    ), { refresh = true }, { handleError(it, rm) })
                            }
                    }
                }
            }
            if (refresh) tasks = routeInteractor.getTaskWithRelations()
            val currentTask = routeInteractor.getCurrentTask()
            handleResult(currentTask, { ct ->
                tasks =
                    tasks.map { t -> t.copy(task = t.task.copy(isCurrent = t.task.id == ct.data.id)) }
            }, {})
            filterAndSetTasksList(tasks, scrollToCurrent)
            taskItemCon = tasks
        }
    }

    private fun filterAndSetTasksList(
        tasks: List<TaskRelations>,
        scrollToCurrent: Boolean = false
    ) {
        var finalList = tasks
        if (isNearFilterOn) {
            finalList = tasks.filter { task ->
                localNearStandsList.map { stand -> stand.id }.contains(task.task.stand?.id ?: 0)
            }
        }
        query?.let { query ->
            finalList = finalList.filter {
                (it.task.stand?.address ?: it.task.visitPoint?.name ?: "").toLowerCase()
                    .contains(query.toLowerCase())
            }
        }
        viewState.setTasksList(finalList, listItem)
        if (scrollToCurrent) {
            launch {
                val cur = routeInteractor.getCurrentTask()
                handleResult(cur, {
                    val pos = finalList.indexOf(it.data)
                    if (pos != 1) {
                        viewState.scrollRvTo(pos)
                    }
                }, { handleError(it, rm) })
            }
        }
        viewState.setEmptyListState(finalList.isEmpty())
        viewState.showLoading(false)
    }

    fun currentTaskButtonClicked() {
        router.navigateTo(Screens.Task())
    }

    fun dumpingButtonClicked() {
        viewState.showDumpingConfirmationDialog()
    }

    fun dumpingConfirmed() {
        viewState.setLoadingState(true)
        launch {
            val polygons = routeInteractor.getPolygonsList()
            handleResult(polygons,
                {
                    viewState.showPolygonSelectionDialog(it.data)
                    viewState.enableButton(true)
                }, {
                    handleError(it, rm)
                    viewState.enableButton(true)
                })

            viewState.setLoadingState(false)
        }
    }

    fun onPolygonSelected(visitPoint: VisitPoint) {
        viewState.showPolygonEnsureDialog(visitPoint)
    }

    fun onPolygonEnsureDialogCancelled() {
        dumpingConfirmed()
    }

    fun onSettingsClicked() {
        viewState.showSettingsMenu(true)
    }

    fun isVisibilityNext(boolean: Int) {
        settingsPrefs.visibilityNext = boolean
    }

    fun onRefund() {
        router.navigateTo(Screens.RouteStart)
    }

    private fun updateTime() {
        val dateFormat = SimpleDateFormat("HH:mm")
        val time = Calendar.getInstance().time
        viewState.showCurrentTime(dateFormat.format(time))
    }

    fun updateBatteryLevel(string: String) {
        viewState.showBatteryLevel(string)
    }

    fun onPolygonEnsured(visitPoint: VisitPoint) {
        viewState.setLoadingState(true)
        launch {
            val result = routeInteractor.startToPolygon(visitPoint.id)

            handleResult(result, {
                viewState.setTasksList(converterMappers.relationsTuTaskExtended(it.data))
                router.newRootScreen(Screens.Dumping)
            }, {
                handleError(it, rm)
            })
            viewState.setLoadingState(false)
        }
    }

    fun nearStandsToggleButtonPressed(isChecked: Boolean, lat: Double, lon: Double) {
        viewState.setLoadingState(true)
        if (isChecked) {
            launch {
                val res = routeInteractor.getNearStands(lat, lon)
                handleResult(res, {
                    isNearFilterOn = true
                    localNearStandsList = it.data
                    loadAndSetTasksList()
                }, {
                    handleError(it, rm)
                })
                viewState.setLoadingState(false)
            }
        } else {
            isNearFilterOn = false
            loadAndSetTasksList()
            viewState.setLoadingState(false)
        }

    }

    fun taskFromListChosen(task: TaskRelations) {
        if (task.task.visitPoint != null) return

        when {
            task.task.isCurrent -> {
                router.replaceScreen(Screens.Task())
            }
            task.task.statusType != StatusType.NEW -> {
                router.navigateTo(Screens.TaskCompleted(task.task.id, taskItemCon))
            }
            else -> {
                router.navigateTo(Screens.Task(task.task.id))
            }
        }
    }

    fun onSearchQueryChanged(text: String?) {
        launch {
            query = text
            loadAndSetTasksList(true)
        }
    }
}
