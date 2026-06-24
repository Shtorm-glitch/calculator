package com.farmers.calc

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.floor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FarmersCalcApp()
        }
    }
}

private enum class Tab(val title: String, val icon: ImageVector) {
    Calc("Расчёт", Icons.Default.Calculate),
    Saved("Сохранённые", Icons.Default.ViewList),
    Settings("Настройки", Icons.Default.Palette)
}

private enum class AccentTheme(val title: String, val seed: Color) {
    Mint("Mint", Color(0xFF1FA67A)),
    Ocean("Ocean", Color(0xFF1677B8)),
    Graphite("Graphite", Color(0xFF7A8491)),
    Berry("Berry", Color(0xFFC13F7A))
}

private data class CalcInput(
    val salary: String = "10000",
    val weight1: String = "60",
    val weight2: String = "20",
    val weight3: String = "20",
    val plan1: String = "100000",
    val plan2: String = "300000",
    val plan3: String = "30",
    val fact1: String = "100000",
    val fact2: String = "300000",
    val fact3: String = "30"
)

private data class CalcResult(
    val percent1: Int,
    val percent2: Int,
    val percent3: Int,
    val bonus1: Int,
    val bonus2: Int,
    val bonus3: Int,
    val beforeTax: Int,
    val afterTax: Int
)

private data class SavedCalc(
    val name: String,
    val savedAt: Long,
    val input: CalcInput,
    val result: CalcResult
)

private enum class ScaleKind {
    Soft,
    Medium
}

private enum class ScaleDialogMode {
    Reference,
    Mine
}

private data class ScaleDialogState(
    val kind: ScaleKind,
    val mode: ScaleDialogMode,
    val kpi: Int
)

private data class ReferenceScaleRow(
    val percent: String,
    val target: String,
    val tone: ScaleTone
)

private data class ResultScaleRow(
    val percent: Int?,
    val target: String,
    val bonus: String,
    val tone: ScaleTone,
    val isCurrent: Boolean = false,
    val isGap: Boolean = false
)

private enum class ScaleTone {
    Positive,
    Neutral,
    Negative
}

private data class FieldErrors(
    val salary: String? = null,
    val weight1: String? = null,
    val weight2: String? = null,
    val weight3: String? = null,
    val plan1: String? = null,
    val plan2: String? = null,
    val plan3: String? = null,
    val fact1: String? = null,
    val fact2: String? = null,
    val fact3: String? = null
) {
    val hasAny: Boolean
        get() = listOf(salary, weight1, weight2, weight3, plan1, plan2, plan3, fact1, fact2, fact3)
            .any { it != null }
}

@Composable
private fun FarmersCalcApp() {
    val context = LocalContext.current
    val store = remember { AppStore(context) }
    var input by remember { mutableStateOf(store.loadInput()) }
    var saved by remember { mutableStateOf(store.loadSaved()) }
    var darkMode by remember { mutableStateOf(store.loadDarkMode()) }
    var accent by remember { mutableStateOf(store.loadAccent()) }

    LaunchedEffect(input) { store.saveInput(input) }
    LaunchedEffect(darkMode) { store.saveDarkMode(darkMode) }
    LaunchedEffect(accent) { store.saveAccent(accent) }

    val colors = colorScheme(accent, darkMode)
    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AppScaffold(
                input = input,
                onInputChange = { input = it },
                saved = saved,
                onSavedChange = {
                    saved = it
                    store.saveSaved(it)
                },
                darkMode = darkMode,
                onDarkModeChange = { darkMode = it },
                accent = accent,
                onAccentChange = { accent = it }
            )
        }
    }
}

@Composable
private fun AppScaffold(
    input: CalcInput,
    onInputChange: (CalcInput) -> Unit,
    saved: List<SavedCalc>,
    onSavedChange: (List<SavedCalc>) -> Unit,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    accent: AccentTheme,
    onAccentChange: (AccentTheme) -> Unit
) {
    var tab by remember { mutableStateOf(Tab.Calc) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (tab) {
                Tab.Calc -> CalcScreen(
                    input = input,
                    onInputChange = onInputChange,
                    saved = saved,
                    onSavedChange = onSavedChange,
                    onSaved = { scope.launch { snackbarHostState.showSnackbar("Расчёт сохранён") } }
                )

                Tab.Saved -> SavedScreen(
                    saved = saved,
                    onSavedChange = onSavedChange,
                    onEdit = {
                        onInputChange(it.input)
                        tab = Tab.Calc
                    }
                )

                Tab.Settings -> SettingsScreen(
                    darkMode = darkMode,
                    onDarkModeChange = onDarkModeChange,
                    accent = accent,
                    onAccentChange = onAccentChange
                )
            }
        }
    }
}

@Composable
private fun CalcScreen(
    input: CalcInput,
    onInputChange: (CalcInput) -> Unit,
    saved: List<SavedCalc>,
    onSavedChange: (List<SavedCalc>) -> Unit,
    onSaved: () -> Unit
) {
    val errors = validate(input)
    val result = if (errors.hasAny) null else calculate(input)
    var showSalaryDialog by remember { mutableStateOf(false) }
    var showWeightsDialog by remember { mutableStateOf(false) }
    var editKpi by remember { mutableStateOf<Int?>(null) }
    var showKpi3Dialog by remember { mutableStateOf(false) }
    var showManualKpi3Dialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var lastValidKpi3Plan by remember { mutableStateOf(if (isValidKpi3Plan(input.plan3)) input.plan3 else "30") }
    LaunchedEffect(input.plan3) {
        if (isValidKpi3Plan(input.plan3)) lastValidKpi3Plan = input.plan3
    }

    fun closeKpi3Dialog() {
        if (!isValidKpi3Plan(input.plan3)) {
            onInputChange(input.copy(plan3 = lastValidKpi3Plan))
        }
        showKpi3Dialog = false
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxHeight < 700.dp
        val tight = maxHeight < 610.dp
        val spacing = if (tight) 6.dp else 10.dp
        val contentPadding = if (tight) 10.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            QuickSettingsRow(
                input = input,
                compact = compact,
                onSalary = { showSalaryDialog = true },
                onWeights = { showWeightsDialog = true }
            )
            KpiPlanFactGrid(
                input = input,
                errors = errors,
                compact = compact,
                onKpiClick = { index ->
                    if (index == 3) showKpi3Dialog = true else editKpi = index
                }
            )
            ResultPanel(
                input = input,
                result = result,
                compact = compact,
                onSave = { if (!errors.hasAny) showSaveDialog = true },
                saveEnabled = result != null
            )
        }
    }

    editKpi?.let { index ->
        EditKpiDialog(
            index = index,
            input = input,
            errors = errors,
            onDismiss = { editKpi = null },
            onInputChange = onInputChange
        )
    }
    if (showKpi3Dialog) {
        val canChooseKpi3 = isValidKpi3Plan(input.plan3)
        Kpi3ChoiceDialog(
            currentCalls = input.fact3.toIntOrNull(),
            plan = input.plan3,
            planInvalid = !canChooseKpi3,
            onPlanChange = { onInputChange(input.copy(plan3 = cleanDigits(it))) },
            onDismiss = { closeKpi3Dialog() },
            onChoose = {
                if (canChooseKpi3) {
                    onInputChange(input.copy(fact3 = it.toString()))
                    showKpi3Dialog = false
                }
            },
            onManual = {
                if (canChooseKpi3) {
                    showKpi3Dialog = false
                    showManualKpi3Dialog = true
                }
            }
        )
    }
    if (showManualKpi3Dialog) {
        EditSingleValueDialog(
            title = "KPI 3 talk time",
            label = "Факт",
            value = input.fact3,
            error = errors.fact3,
            onDismiss = { showManualKpi3Dialog = false },
            onChange = { onInputChange(input.copy(fact3 = cleanDigits(it))) }
        )
    }

    if (showSalaryDialog) {
        EditFormattedSingleValueDialog(
            title = "Оклад",
            label = "Оклад",
            value = input.salary,
            error = errors.salary,
            onDismiss = { showSalaryDialog = false },
            onChange = { onInputChange(input.copy(salary = cleanDigits(it))) }
        )
    }
    if (showWeightsDialog) {
        EditWeightsDialog(
            input = input,
            errors = errors,
            onDismiss = { showWeightsDialog = false },
            onInputChange = onInputChange
        )
    }
    if (showSaveDialog && result != null) {
        SaveDialog(
            saved = saved,
            input = input,
            result = result,
            onDismiss = { showSaveDialog = false },
            onSaved = {
                onSavedChange(it)
                showSaveDialog = false
                onSaved()
            }
        )
    }
}

@Composable
private fun QuickSettingsRow(input: CalcInput, compact: Boolean, onSalary: () -> Unit, onWeights: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
        SettingChip("Оклад: ${formatRub(input.salary.toIntOrNull() ?: 0)}", onSalary)
        WeightBoxRow(input = input, compact = compact, onClick = onWeights)
    }
}

@Composable
private fun SettingChip(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun WeightBoxRow(input: CalcInput, compact: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val labelWidth = if (compact) 34.dp else 42.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, accent, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(if (compact) 8.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
    ) {
        Text("Веса KPI", fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(labelWidth))
            WeightCell("KPI 1", input.weight1, compact, Modifier.weight(1f))
            WeightCell("KPI 2", input.weight2, compact, Modifier.weight(1f))
            WeightCell("KPI 3", input.weight3, compact, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WeightCell(label: String, value: String, compact: Boolean, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 34.dp else 42.dp)
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$value%",
                fontSize = if (compact) 14.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun KpiPlanFactGrid(
    input: CalcInput,
    errors: FieldErrors,
    compact: Boolean,
    onKpiClick: (Int) -> Unit
) {
    val labelWidth = if (compact) 34.dp else 42.dp
    val cellHeight = if (compact) 58.dp else 68.dp
    val titleSize = if (compact) 11.sp else 12.sp
    val valueSize = if (compact) 14.sp else 16.sp
    val kpis = listOf(
        Triple("KPI 1 AV", input.plan1, input.fact1),
        Triple("KPI 2 total", input.plan2, input.fact2),
        Triple("KPI 3 talk time", input.plan3, input.fact3)
    )
    val hasError = listOf(errors.plan1, errors.fact1, errors.plan2, errors.fact2, errors.plan3, errors.fact3).any { it != null }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Spacer(Modifier.width(labelWidth))
            kpis.forEach { (title, _, _) ->
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(
                modifier = Modifier
                    .width(labelWidth)
                    .height(cellHeight),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("План", fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text("Факт", fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
            kpis.forEachIndexed { index, item ->
                KpiCompactCell(
                    plan = item.second,
                    fact = item.third,
                    cellHeight = cellHeight,
                    valueSize = valueSize,
                    modifier = Modifier.weight(1f),
                    onClick = { onKpiClick(index + 1) }
                )
            }
        }
        if (hasError) {
            val message = when {
                errors.plan1 != null || errors.fact1 != null -> "Проверьте KPI 1"
                errors.plan2 != null || errors.fact2 != null -> "Проверьте KPI 2"
                else -> "Проверьте KPI 3"
            }
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        }
    }
}

@Composable
private fun KpiCompactCell(
    plan: String,
    fact: String,
    cellHeight: androidx.compose.ui.unit.Dp,
    valueSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(cellHeight)
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(compactNumber(plan), fontSize = valueSize, fontWeight = FontWeight.Bold, maxLines = 1)
            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
            Text(compactNumber(fact), fontSize = valueSize, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    error: String?,
    onChange: (String) -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        trailingIcon = trailing,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun FormattedNumberField(
    label: String,
    value: String,
    error: String?,
    onChange: (String) -> Unit
) {
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }

    LaunchedEffect(value) {
        if (fieldValue.text != value) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = fieldValue,
        onValueChange = { incoming ->
            val cursor = incoming.text
                .take(incoming.selection.end.coerceIn(0, incoming.text.length))
                .count { it.isDigit() }
            val digits = cleanDigits(incoming.text)
            fieldValue = TextFieldValue(digits, TextRange(cursor.coerceIn(0, digits.length)))
            onChange(digits)
        },
        label = { Text(label) },
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        singleLine = true,
        visualTransformation = GroupedDigitsTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

private object GroupedDigitsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val transformed = formatGroupedDigits(original)
        return TransformedText(
            AnnotatedString(transformed),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return formatGroupedDigits(original.take(offset.coerceIn(0, original.length))).length
                }

                override fun transformedToOriginal(offset: Int): Int {
                    return transformed
                        .take(offset.coerceIn(0, transformed.length))
                        .count { it.isDigit() }
                        .coerceIn(0, original.length)
                }
            }
        )
    }
}

@Composable
private fun ResultPanel(
    input: CalcInput,
    result: CalcResult?,
    compact: Boolean,
    onSave: () -> Unit,
    saveEnabled: Boolean
) {
    var scaleDialog by remember { mutableStateOf<ScaleDialogState?>(null) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f), RoundedCornerShape(8.dp)),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
        ) {
            if (result == null) {
                Text("Заполните все поля корректно", color = MaterialTheme.colorScheme.error)
            } else {
                ResultLine(
                    title = "KPI 1",
                    middle = "${result.percent1}%",
                    value = formatRub(result.bonus1),
                    scaleTitle = "\u0428\u043a\u0430\u043b\u0430",
                    onScaleClick = { scaleDialog = ScaleDialogState(ScaleKind.Soft, ScaleDialogMode.Reference, 1) },
                    onMineClick = { scaleDialog = ScaleDialogState(ScaleKind.Soft, ScaleDialogMode.Mine, 1) }
                )
                ResultLine(
                    title = "KPI 2",
                    middle = "${result.percent2}%",
                    value = formatRub(result.bonus2),
                    scaleTitle = "\u0428\u043a\u0430\u043b\u0430",
                    onScaleClick = { scaleDialog = ScaleDialogState(ScaleKind.Medium, ScaleDialogMode.Reference, 2) },
                    onMineClick = { scaleDialog = ScaleDialogState(ScaleKind.Medium, ScaleDialogMode.Mine, 2) }
                )
                ResultLine(
                    title = "KPI 3",
                    middle = "${result.percent3}%",
                    value = formatRub(result.bonus3),
                    scaleTitle = "\u0428\u043a\u0430\u043b\u0430",
                    onScaleClick = { scaleDialog = ScaleDialogState(ScaleKind.Soft, ScaleDialogMode.Reference, 3) },
                    onMineClick = { scaleDialog = ScaleDialogState(ScaleKind.Soft, ScaleDialogMode.Mine, 3) }
                )
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                ResultLine("Премия до налога", "", formatRub(result.beforeTax), strong = true)
                ResultLine("За вычетом 13%", "", formatRub(result.afterTax), strong = true)
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = saveEnabled,
                onClick = onSave
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Сохранить расчёт")
            }
        }
    }

    val opened = scaleDialog
    if (opened != null && result != null) {
        ScaleInfoDialog(
            state = opened,
            currentPercent = when (opened.kpi) {
                1 -> result.percent1
                2 -> result.percent2
                else -> result.percent3
            },
            baseBonus = when (opened.kpi) {
                1 -> weightedBase(input.salary, input.weight1)
                2 -> weightedBase(input.salary, input.weight2)
                else -> weightedBase(input.salary, input.weight3)
            },
            onDismiss = { scaleDialog = null }
        )
    }
}

@Composable
private fun ResultLine(
    title: String,
    middle: String,
    value: String,
    strong: Boolean = false,
    scaleTitle: String? = null,
    onScaleClick: (() -> Unit)? = null,
    onMineClick: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onMineClick != null) {
            ResultKpiButton(title, onMineClick)
            Spacer(Modifier.width(8.dp))
            if (scaleTitle != null && onScaleClick != null) {
                ResultScaleButton(scaleTitle, onScaleClick)
            }
        } else {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal
            )
        }
        if (onMineClick != null) Spacer(Modifier.weight(1f))
        if (middle.isNotBlank()) {
            Text(text = middle, modifier = Modifier.padding(horizontal = 6.dp))
        }
        Text(text = value, fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium)
        if (onMineClick == null && scaleTitle != null && onScaleClick != null) {
            Spacer(Modifier.width(8.dp))
            ResultScaleButton(scaleTitle, onScaleClick)
        }
    }
}

@Composable
private fun ResultKpiButton(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(32.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.62f), RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
            Text(
                text = "\$",
                color = Color(0xFF2FB36F),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ResultScaleButton(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(32.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.62f), RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ScaleInfoDialog(
    state: ScaleDialogState,
    currentPercent: Int,
    baseBonus: Double,
    onDismiss: () -> Unit
) {
    val isMine = state.mode == ScaleDialogMode.Mine
    val title = when {
        isMine -> "KPI ${state.kpi}: \u043c\u043e\u044f \u0448\u043a\u0430\u043b\u0430"
        state.kind == ScaleKind.Soft -> "\u0428\u043a\u0430\u043b\u0430 \u043f\u043e\u043b\u043e\u0433\u0430\u044f"
        else -> "\u0428\u043a\u0430\u043b\u0430 \u0441\u0440\u0435\u0434\u043d\u044f\u044f"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Normal) },
        text = {
            if (isMine) {
                ResultScaleTable(
                    rows = resultScaleRows(
                        currentPercent = currentPercent,
                        baseBonus = baseBonus,
                        kind = state.kind
                    )
                )
            } else {
                ReferenceScaleTable(rows = referenceScaleRows(state.kind))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("\u0417\u0430\u043a\u0440\u044b\u0442\u044c") } }
    )
}

@Composable
private fun ReferenceScaleTable(rows: List<ReferenceScaleRow>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .verticalScroll(rememberScrollState())
    ) {
        ScaleHeader(listOf("% \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f", "% \u043e\u0442 \u0446\u0435\u043b\u0435\u0432\u043e\u0439"))
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scaleToneColor(row.tone))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                ScaleCell(row.percent, Modifier.weight(0.9f), TextAlign.Center)
                ScaleCell(row.target, Modifier.weight(1.35f), TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ResultScaleTable(rows: List<ResultScaleRow>) {
    val currentIndex = rows.indexOfFirst { it.isCurrent }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 3).coerceAtLeast(0))

    LaunchedEffect(currentIndex) {
        listState.scrollToItem((currentIndex - 3).coerceAtLeast(0))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
    ) {
        ScaleHeader(listOf("% \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f", "% \u043e\u0442 \u0446\u0435\u043b\u0435\u0432\u043e\u0439", "\u0421\u0443\u043c\u043c\u0430 \u043f\u0440\u0435\u043c\u0438\u0438"))
        LazyColumn(state = listState) {
            itemsIndexed(rows) { _, row ->
                if (row.isGap) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("...", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val borderColor = if (row.isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (row.isCurrent) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                                } else {
                                    scaleToneColor(row.tone)
                                }
                            )
                            .border(if (row.isCurrent) 2.dp else 0.5.dp, borderColor)
                    ) {
                        ScaleCell("${row.percent}%", Modifier.weight(0.9f), TextAlign.Center)
                        ScaleCell(row.target, Modifier.weight(0.9f), TextAlign.Right)
                        ScaleCell(row.bonus, Modifier.weight(1.1f), TextAlign.Right)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScaleHeader(titles: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        titles.forEachIndexed { index, title ->
            ScaleCell(
                text = title,
                modifier = Modifier.weight(if (titles.size == 2 && index == 1) 1.35f else if (titles.size == 2) 0.9f else if (index == 2) 1.1f else 0.9f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ScaleCell(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        textAlign = textAlign,
        fontWeight = fontWeight,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun scaleToneColor(tone: ScaleTone): Color {
    return when (tone) {
        ScaleTone.Positive -> Color(0xFF2FB36F).copy(alpha = if (MaterialTheme.colorScheme.background == Color(0xFF101113)) 0.25f else 0.18f)
        ScaleTone.Neutral -> Color(0xFFF0C84B).copy(alpha = 0.18f)
        ScaleTone.Negative -> Color(0xFFDF795F).copy(alpha = if (MaterialTheme.colorScheme.background == Color(0xFF101113)) 0.24f else 0.17f)
    }
}

@Composable
private fun EditSingleValueDialog(
    title: String,
    label: String,
    value: String,
    error: String?,
    onDismiss: () -> Unit,
    onChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            NumberField(label = label, value = value, error = error, onChange = onChange)
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } }
    )
}

@Composable
private fun EditFormattedSingleValueDialog(
    title: String,
    label: String,
    value: String,
    error: String?,
    onDismiss: () -> Unit,
    onChange: (String) -> Unit
) {
    var draft by remember(value) { mutableStateOf(value) }
    fun commitAndDismiss() {
        onChange(cleanDigits(draft))
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = { commitAndDismiss() },
        title = { Text(title) },
        text = {
            FormattedNumberField(label = label, value = draft, error = error, onChange = { draft = it })
        },
        confirmButton = { TextButton(onClick = { commitAndDismiss() }) { Text("Готово") } }
    )
}

@Composable
private fun EditWeightsDialog(input: CalcInput, errors: FieldErrors, onDismiss: () -> Unit, onInputChange: (CalcInput) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Веса KPI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("KPI 1 AV, %", input.weight1, errors.weight1, { onInputChange(input.copy(weight1 = cleanDigits(it))) })
                NumberField("KPI 2 тотал revenue, %", input.weight2, errors.weight2, { onInputChange(input.copy(weight2 = cleanDigits(it))) })
                NumberField("KPI 3 talk time, %", input.weight3, errors.weight3, { onInputChange(input.copy(weight3 = cleanDigits(it))) })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } }
    )
}

@Composable
private fun EditKpiDialog(index: Int, input: CalcInput, errors: FieldErrors, onDismiss: () -> Unit, onInputChange: (CalcInput) -> Unit) {
    val title = if (index == 1) "KPI 1 AV" else "KPI 2 тотал revenue"
    var draft by remember(index, input) { mutableStateOf(input) }
    val draftErrors = validate(draft)
    fun commitAndDismiss() {
        onInputChange(draft)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = { commitAndDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (index == 1) {
                    FormattedNumberField("План", draft.plan1, draftErrors.plan1, { draft = draft.copy(plan1 = it) })
                    FormattedNumberField("Факт", draft.fact1, draftErrors.fact1, { draft = draft.copy(fact1 = it) })
                } else {
                    FormattedNumberField("План", draft.plan2, draftErrors.plan2, { draft = draft.copy(plan2 = it) })
                    FormattedNumberField("Факт", draft.fact2, draftErrors.fact2, { draft = draft.copy(fact2 = it) })
                }
            }
        },
        confirmButton = { TextButton(onClick = { commitAndDismiss() }) { Text("Готово") } }
    )
}

@Composable
private fun Kpi3ChoiceDialog(
    currentCalls: Int?,
    plan: String,
    planInvalid: Boolean,
    onPlanChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onChoose: (Int) -> Unit,
    onManual: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("KPI 3 talk time") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = plan,
                    onValueChange = { onPlanChange(cleanDigits(it).take(3)) },
                    label = { Text("План") },
                    isError = planInvalid,
                    supportingText = {
                        if (planInvalid) Text("Введите целое число от 1 до 99")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Column(
                    modifier = Modifier
                        .height(360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    talkTimeRows.reversed().forEach { (calls, minutes) ->
                        val highlighted = currentCalls == calls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !planInvalid) { onChoose(calls) }
                                .background(
                                    if (highlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "$calls звонков",
                                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                                color = if (planInvalid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else Color.Unspecified
                            )
                            Text(
                                "$minutes минут",
                                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                                color = if (planInvalid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else Color.Unspecified
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !planInvalid, onClick = onManual)
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Ввести вручную",
                            color = if (planInvalid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

@Composable
private fun SaveDialog(
    saved: List<SavedCalc>,
    input: CalcInput,
    result: CalcResult,
    onDismiss: () -> Unit,
    onSaved: (List<SavedCalc>) -> Unit
) {
    var name by remember { mutableStateOf(defaultSaveName()) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var replacing by remember { mutableStateOf(false) }

    if (replacing) {
        AlertDialog(
            onDismissRequest = { replacing = false },
            title = { Text("Выберите расчёт для замены") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    saved.forEachIndexed { index, item ->
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val newItem = SavedCalc(name.trim(), System.currentTimeMillis(), input, result)
                                onSaved(saved.toMutableList().also { it[index] = newItem })
                            }
                        ) { Text(item.name) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { replacing = false }) { Text("Назад") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Название") },
        text = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = {
                    name = it.take(30)
                    nameError = null
                },
                isError = nameError != null,
                supportingText = { if (nameError != null) Text(nameError!!) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanName = name.trim()
                    nameError = when {
                        cleanName.isBlank() -> "Введите название"
                        saved.any { it.name == cleanName } -> "Такое название уже есть"
                        else -> null
                    }
                    if (nameError == null) {
                        val newItem = SavedCalc(cleanName, System.currentTimeMillis(), input, result)
                        if (saved.size < 3) onSaved(saved + newItem) else replacing = true
                    }
                }
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun SavedScreen(saved: List<SavedCalc>, onSavedChange: (List<SavedCalc>) -> Unit, onEdit: (SavedCalc) -> Unit) {
    var opened by remember { mutableStateOf<SavedCalc?>(null) }
    var deleteTarget by remember { mutableStateOf<SavedCalc?>(null) }

    if (saved.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Сохранённых расчётов пока нет")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(saved) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { opened = item },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        IconButton(onClick = { deleteTarget = item }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
    }

    opened?.let { item ->
        SavedDetailsDialog(
            item = item,
            onDismiss = { opened = null },
            onEdit = {
                opened = null
                onEdit(item)
            }
        )
    }
    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Удалить расчёт?") },
            text = { Text(item.name) },
            confirmButton = {
                TextButton(onClick = {
                    onSavedChange(saved.filterNot { it.name == item.name && it.savedAt == item.savedAt })
                    deleteTarget = null
                    opened = null
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun SavedDetailsDialog(item: SavedCalc, onDismiss: () -> Unit, onEdit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ResultLine("KPI 1 AV", "${item.result.percent1}%", formatRub(item.result.bonus1))
                ResultLine("KPI 2 тотал revenue", "${item.result.percent2}%", formatRub(item.result.bonus2))
                ResultLine("KPI 3 talk time", "${item.result.percent3}%", formatRub(item.result.bonus3))
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                ResultLine("Премия до налога", "", formatRub(item.result.beforeTax), strong = true)
                ResultLine("За вычетом 13%", "", formatRub(item.result.afterTax), strong = true)
            }
        },
        confirmButton = {
            Button(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Изменить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

@Composable
private fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    accent: AccentTheme,
    onAccentChange: (AccentTheme) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SettingsCard("Оформление") {
                ChoiceRow("Тёмная", darkMode, { onDarkModeChange(true) })
                ChoiceRow("Светлая", !darkMode, { onDarkModeChange(false) })
            }
        }
        item {
            SettingsCard("Цветовая тема") {
                AccentTheme.entries.forEach { theme ->
                    ChoiceRow(theme.title, accent == theme, { onAccentChange(theme) }, color = theme.seed)
                }
            }
        }
        item {
            SettingsCard("Версия") {
                Text("Версия 1.0")
                Spacer(Modifier.height(4.dp))
                Text("Дата релиза: ${BuildConfig.RELEASE_DATE}")
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ChoiceRow(text: String, selected: Boolean, onClick: () -> Unit, color: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        if (color != null) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(color, RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
}

private fun validate(input: CalcInput): FieldErrors {
    fun positive(value: String, label: String): String? {
        val number = value.toLongOrNull() ?: return "$label: заполните поле"
        return when {
            number <= 0 -> "$label: должно быть больше 0"
            number > 1_000_000_000L -> "$label: максимум 1 000 000 000"
            else -> null
        }
    }

    val weight1 = positive(input.weight1, "Вес KPI 1") ?: weightRange(input.weight1, "Вес KPI 1")
    val weight2 = positive(input.weight2, "Вес KPI 2") ?: weightRange(input.weight2, "Вес KPI 2")
    val weight3 = positive(input.weight3, "Вес KPI 3") ?: weightRange(input.weight3, "Вес KPI 3")
    val weightSum = listOf(input.weight1, input.weight2, input.weight3).mapNotNull { it.toIntOrNull() }.sum()
    val sumError = if (weight1 == null && weight2 == null && weight3 == null && weightSum != 100) {
        "Сумма весов должна быть 100%"
    } else null

    return FieldErrors(
        salary = positive(input.salary, "Оклад"),
        weight1 = weight1 ?: sumError,
        weight2 = weight2 ?: sumError,
        weight3 = weight3 ?: sumError,
        plan1 = positive(input.plan1, "План KPI 1"),
        plan2 = positive(input.plan2, "План KPI 2"),
        plan3 = kpi3PlanError(input.plan3),
        fact1 = positive(input.fact1, "Факт KPI 1"),
        fact2 = positive(input.fact2, "Факт KPI 2"),
        fact3 = positive(input.fact3, "Факт KPI 3")
    )
}

private fun weightRange(value: String, label: String): String? {
    val number = value.toIntOrNull() ?: return "$label: заполните поле"
    return if (number !in 1..100) "$label: от 1 до 100%" else null
}

private fun calculate(input: CalcInput): CalcResult {
    val salary = input.salary.toDouble()
    fun ratio(fact: String, plan: String): Double {
        return floor((fact.toDouble() / plan.toDouble()) * 100.0) / 100.0
    }

    val ratio1 = ratio(input.fact1, input.plan1)
    val ratio2 = ratio(input.fact2, input.plan2)
    val ratio3 = ratio(input.fact3, input.plan3)
    val bonus1 = floor(salary * input.weight1.toDouble() / 100.0 * scaleCoefficient(ratio1)).toInt()
    val bonus2 = floor(salary * input.weight2.toDouble() / 100.0 * mediumScaleCoefficient(ratio2)).toInt()
    val bonus3 = floor(salary * input.weight3.toDouble() / 100.0 * scaleCoefficient(ratio3)).toInt()
    val beforeTax = bonus1 + bonus2 + bonus3
    val afterTax = floor(beforeTax * 0.87).toInt()

    return CalcResult(
        percent1 = floor(ratio1 * 100).toInt(),
        percent2 = floor(ratio2 * 100).toInt(),
        percent3 = floor(ratio3 * 100).toInt(),
        bonus1 = bonus1,
        bonus2 = bonus2,
        bonus3 = bonus3,
        beforeTax = beforeTax,
        afterTax = afterTax
    )
}

private fun scaleCoefficient(ratio: Double): Double {
    val percent = ratioPercent(ratio)
    return when {
        percent <= 69 -> 0.0
        percent <= 74 -> 0.20
        percent <= 79 -> 0.25
        percent <= 84 -> 0.35
        percent <= 89 -> (70 - (90 - percent) * 2) / 100.0
        percent <= 99 -> (100 - (100 - percent) * 3) / 100.0
        percent <= 114 -> (100 + (percent - 100) * 3) / 100.0
        percent <= 120 -> (142 + (percent - 114) * 2) / 100.0
        else -> (154 + (percent - 120) * 0.5) / 100.0
    }
}

private fun mediumScaleCoefficient(ratio: Double): Double {
    val percent = ratioPercent(ratio)
    return when {
        percent <= 79 -> 0.0
        percent <= 84 -> 0.20
        percent <= 89 -> 0.30
        percent <= 94 -> (75 - (95 - percent) * 4) / 100.0
        percent <= 99 -> (100 - (100 - percent) * 5) / 100.0
        percent <= 105 -> (100 + (percent - 100) * 5) / 100.0
        percent <= 110 -> (125 + (percent - 105) * 4) / 100.0
        percent <= 120 -> (145 + (percent - 110) * 3) / 100.0
        percent <= 129 -> (175 + (percent - 120)) / 100.0
        else -> (184 + (percent - 129) * 0.5) / 100.0
    }
}

private fun ratioPercent(ratio: Double): Int = floor(ratio * 100.0 + 1e-9).toInt()

private fun weightedBase(salary: String, weight: String): Double {
    return salary.toDoubleOrNull().orZero() * weight.toDoubleOrNull().orZero() / 100.0
}

private fun referenceScaleRows(kind: ScaleKind): List<ReferenceScaleRow> {
    return when (kind) {
        ScaleKind.Soft -> listOf(
            ReferenceScaleRow("121% \u0438 \u0431\u043e\u043b\u0435\u0435", "0,5% \u0437\u0430 \u043a\u0430\u0436\u0434\u044b\u0439 % \u043f\u0435\u0440\u0435\u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f", ScaleTone.Positive),
            ReferenceScaleRow("115-120%", "2% \u0437\u0430 \u043a\u0430\u0436\u0434\u044b\u0439 % \u043f\u0435\u0440\u0435\u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f", ScaleTone.Positive),
            ReferenceScaleRow("101-114%", "3% \u0437\u0430 \u043a\u0430\u0436\u0434\u044b\u0439 % \u043f\u0435\u0440\u0435\u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f", ScaleTone.Positive),
            ReferenceScaleRow("100%", "100%", ScaleTone.Neutral),
            ReferenceScaleRow("90-99%", "-3% \u0437\u0430 \u043a\u0430\u0436\u0434\u044b\u0439 % \u043d\u0435\u0434\u043e\u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f", ScaleTone.Negative),
            ReferenceScaleRow("85-89%", "-2% \u0437\u0430 \u043a\u0430\u0436\u0434\u044b\u0439 % \u043d\u0435\u0434\u043e\u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f", ScaleTone.Negative),
            ReferenceScaleRow("80-84%", "35%", ScaleTone.Negative),
            ReferenceScaleRow("75-79%", "25%", ScaleTone.Negative),
            ReferenceScaleRow("70-74%", "20%", ScaleTone.Negative),
            ReferenceScaleRow("69% \u0438 \u043c\u0435\u043d\u0435\u0435", "0%", ScaleTone.Negative)
        )
        ScaleKind.Medium -> listOf(
            ReferenceScaleRow("130% \u0438 \u0431\u043e\u043b\u0435\u0435", "0,5% \u0437\u0430 \u043a\u0430\u0436\u0434\u044b\u0439 % \u043f\u0435\u0440\u0435\u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f", ScaleTone.Positive),
            ReferenceScaleRow("121-129%", "1% \u0437\u0430 \u043a\u0430\u0436\u0434\u044b\u0439 % \u043f\u0435\u0440\u0435\u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f", ScaleTone.Positive),
            ReferenceScaleRow("111-120%", "3% \u0437\u0430 \u043a\u0430\u0436\u0434\u044b\u0439 % \u043f\u0435\u0440\u0435\u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f", ScaleTone.Positive),
            ReferenceScaleRow("106-110%", "4% \u0437\u0430 \u043a\u0430\u0436\u0434\u044b\u0439 % \u043f\u0435\u0440\u0435\u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f", ScaleTone.Positive),
            ReferenceScaleRow("101-105%", "5% \u0437\u0430 \u043a\u0430\u0436\u0434\u044b\u0439 % \u043f\u0435\u0440\u0435\u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f", ScaleTone.Positive),
            ReferenceScaleRow("100%", "100%", ScaleTone.Neutral),
            ReferenceScaleRow("95-99%", "-5% \u0437\u0430 \u043a\u0430\u0436\u0434\u044b\u0439 % \u043d\u0435\u0434\u043e\u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f", ScaleTone.Negative),
            ReferenceScaleRow("90-94%", "-4% \u0437\u0430 \u043a\u0430\u0436\u0434\u044b\u0439 % \u043d\u0435\u0434\u043e\u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f", ScaleTone.Negative),
            ReferenceScaleRow("85-89%", "30%", ScaleTone.Negative),
            ReferenceScaleRow("80-84%", "20%", ScaleTone.Negative),
            ReferenceScaleRow("79% \u0438 \u043c\u0435\u043d\u0435\u0435", "0%", ScaleTone.Negative)
        )
    }
}

private fun resultScaleRows(currentPercent: Int, baseBonus: Double, kind: ScaleKind): List<ResultScaleRow> {
    fun row(percent: Int, current: Boolean = false): ResultScaleRow {
        val coefficient = if (kind == ScaleKind.Soft) {
            scaleCoefficient(percent / 100.0)
        } else {
            mediumScaleCoefficient(percent / 100.0)
        }
        return ResultScaleRow(
            percent = percent,
            target = "${floor(coefficient * 100 + 1e-9).toInt()}%",
            bonus = formatRub(floor(baseBonus * coefficient).toInt()),
            tone = when {
                percent > 100 -> ScaleTone.Positive
                percent == 100 -> ScaleTone.Neutral
                else -> ScaleTone.Negative
            },
            isCurrent = current
        )
    }

    val standard = (121 downTo 69).map { row(it, it == currentPercent) }
    val gap = ResultScaleRow(null, "", "", ScaleTone.Neutral, isGap = true)

    return when {
        currentPercent > 121 -> listOf(row(currentPercent, true), gap) + standard
        currentPercent < 69 -> standard + gap + row(currentPercent, true)
        else -> standard
    }
}

private fun Double?.orZero(): Double = this ?: 0.0

private fun colorScheme(accent: AccentTheme, darkMode: Boolean) = if (darkMode) {
    darkColorScheme(
        primary = accent.seed,
        onPrimary = Color.White,
        primaryContainer = accent.seed.copy(alpha = 0.32f),
        onPrimaryContainer = Color(0xFFF6F8FA),
        secondary = accent.seed.copy(alpha = 0.82f),
        secondaryContainer = accent.seed.copy(alpha = 0.22f),
        outline = accent.seed.copy(alpha = 0.72f),
        surface = Color(0xFF17191C),
        surfaceVariant = Color(0xFF24272C),
        background = Color(0xFF101113)
    )
} else {
    lightColorScheme(
        primary = accent.seed,
        onPrimary = Color.White,
        primaryContainer = accent.seed.copy(alpha = 0.18f),
        onPrimaryContainer = Color(0xFF17191C),
        secondary = accent.seed.copy(alpha = 0.82f),
        secondaryContainer = accent.seed.copy(alpha = 0.14f),
        outline = accent.seed.copy(alpha = 0.68f),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF0F2F4),
        background = Color(0xFFFAFAFA)
    )
}

private val talkTimeRows = (20..35).map { calls -> calls to (130 + (calls - 20) * 5) }

private fun kpi3PlanError(value: String): String? {
    val number = value.toIntOrNull() ?: return "Введите целое число от 1 до 99"
    return if (number in 1..99) null else "Введите целое число от 1 до 99"
}

private fun isValidKpi3Plan(value: String): Boolean = kpi3PlanError(value) == null

private fun compactNumber(value: String): String {
    val number = value.toLongOrNull() ?: return value
    return when {
        number >= 1_000_000 -> {
            val millions = number / 1_000_000.0
            if (number % 1_000_000L == 0L) "${number / 1_000_000}M" else "%.1fM".format(java.util.Locale.US, millions)
        }
        number >= 1_000 -> "${number / 1_000}k"
        else -> number.toString()
    }
}

private fun formatGroupedDigits(value: String): String {
    val digits = value.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    return digits.reversed().chunked(3).joinToString(" ").reversed()
}

private fun cursorAfterDigits(text: String, digitCount: Int): Int {
    if (digitCount <= 0) return 0
    var seen = 0
    text.forEachIndexed { index, char ->
        if (char.isDigit()) {
            seen++
            if (seen == digitCount) return index + 1
        }
    }
    return text.length
}

private fun cleanDigits(text: String): String {
    return text.filter { it.isDigit() }.take(10).let {
        if ((it.toLongOrNull() ?: 0L) > 1_000_000_000L) "1000000000" else it
    }
}

private fun formatRub(value: Int): String = "%,d ₽".format(value).replace(',', ' ')

private fun defaultSaveName(): String {
    val formatter = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale("ru", "RU"))
    return formatter.format(java.util.Date())
}

private class AppStore(context: Context) {
    private val prefs = context.getSharedPreferences("farmers_calc", Context.MODE_PRIVATE)

    fun loadInput(): CalcInput = prefs.getString("input", null)?.let(::inputFromJson) ?: CalcInput()
    fun saveInput(input: CalcInput) {
        prefs.edit().putString("input", inputToJson(input).toString()).apply()
    }

    fun loadSaved(): List<SavedCalc> {
        val raw = prefs.getString("saved", "[]") ?: "[]"
        val array = JSONArray(raw)
        return List(array.length()) { savedFromJson(array.getJSONObject(it)) }
    }

    fun saveSaved(items: List<SavedCalc>) {
        val array = JSONArray()
        items.forEach { array.put(savedToJson(it)) }
        prefs.edit().putString("saved", array.toString()).apply()
    }

    fun loadDarkMode(): Boolean = prefs.getBoolean("dark", true)
    fun saveDarkMode(value: Boolean) {
        prefs.edit().putBoolean("dark", value).apply()
    }

    fun loadAccent(): AccentTheme = runCatching {
        AccentTheme.valueOf(prefs.getString("accent", AccentTheme.Graphite.name) ?: AccentTheme.Graphite.name)
    }.getOrDefault(AccentTheme.Graphite)

    fun saveAccent(value: AccentTheme) {
        prefs.edit().putString("accent", value.name).apply()
    }
}

private fun inputToJson(input: CalcInput) = JSONObject()
    .put("salary", input.salary)
    .put("weight1", input.weight1)
    .put("weight2", input.weight2)
    .put("weight3", input.weight3)
    .put("plan1", input.plan1)
    .put("plan2", input.plan2)
    .put("plan3", input.plan3)
    .put("fact1", input.fact1)
    .put("fact2", input.fact2)
    .put("fact3", input.fact3)

private fun inputFromJson(json: String): CalcInput {
    val obj = JSONObject(json)
    return CalcInput(
        salary = obj.optString("salary", "10000"),
        weight1 = obj.optString("weight1", "60"),
        weight2 = obj.optString("weight2", "20"),
        weight3 = obj.optString("weight3", "20"),
        plan1 = obj.optString("plan1", "100000"),
        plan2 = obj.optString("plan2", "300000"),
        plan3 = obj.optString("plan3", "30"),
        fact1 = obj.optString("fact1", "100000"),
        fact2 = obj.optString("fact2", "300000"),
        fact3 = obj.optString("fact3", "30")
    )
}

private fun resultToJson(result: CalcResult) = JSONObject()
    .put("percent1", result.percent1)
    .put("percent2", result.percent2)
    .put("percent3", result.percent3)
    .put("bonus1", result.bonus1)
    .put("bonus2", result.bonus2)
    .put("bonus3", result.bonus3)
    .put("beforeTax", result.beforeTax)
    .put("afterTax", result.afterTax)

private fun resultFromJson(obj: JSONObject) = CalcResult(
    percent1 = obj.optInt("percent1"),
    percent2 = obj.optInt("percent2"),
    percent3 = obj.optInt("percent3"),
    bonus1 = obj.optInt("bonus1"),
    bonus2 = obj.optInt("bonus2"),
    bonus3 = obj.optInt("bonus3"),
    beforeTax = obj.optInt("beforeTax"),
    afterTax = obj.optInt("afterTax")
)

private fun savedToJson(item: SavedCalc) = JSONObject()
    .put("name", item.name)
    .put("savedAt", item.savedAt)
    .put("input", inputToJson(item.input))
    .put("result", resultToJson(item.result))

private fun savedFromJson(obj: JSONObject) = SavedCalc(
    name = obj.optString("name"),
    savedAt = obj.optLong("savedAt"),
    input = inputFromJson(obj.getJSONObject("input").toString()),
    result = resultFromJson(obj.getJSONObject("result"))
)
