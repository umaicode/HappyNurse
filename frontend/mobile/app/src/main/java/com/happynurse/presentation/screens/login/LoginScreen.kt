// 로그인 — 병원/병동 드롭다운(API), 사원번호/비밀번호 입력, 지문/Face ID 버튼
package com.happynurse.presentation.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happynurse.data.remote.model.OrganizationDto
import com.happynurse.data.remote.model.WardDto
import com.happynurse.presentation.components.HnButton
import com.happynurse.presentation.components.HnButtonVariant
import com.happynurse.presentation.theme.HnColors

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLoggedIn()
    }

    val valid = state.selectedOrg != null &&
        state.selectedWard != null &&
        state.employeeNumber.isNotBlank() &&
        state.password.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HnColors.Surface)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.weight(1f))

        // 로고
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(HnColors.Primary),
            ) {
                Icon(Icons.Outlined.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("HappyNurse", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = HnColors.Primary, letterSpacing = (-0.03).sp)
            Text("기록은 우리가, 케어는 간호사가", fontSize = 14.sp, color = HnColors.TextSecondary, modifier = Modifier.padding(top = 6.dp))
        }

        Spacer(Modifier.height(28.dp))

        // 병원 선택
        SelectField(
            icon = Icons.Outlined.Apartment,
            label = if (state.organizations.isEmpty()) "병원 목록 불러오는 중…" else "소속병원 선택",
            value = state.selectedOrg?.name ?: "",
            options = state.organizations.map { it.name },
            onSelect = { name ->
                val org = state.organizations.firstOrNull { it.name == name }
                if (org != null) viewModel.selectOrganization(org)
            },
        )
        Spacer(Modifier.height(10.dp))

        // 병동 선택
        SelectField(
            icon = Icons.Outlined.Layers,
            label = if (state.selectedOrg == null) "병원을 먼저 선택하세요" else "근무병동 선택",
            value = state.selectedWard?.wardName ?: "",
            options = state.wards.map { it.wardName },
            onSelect = { name ->
                val ward = state.wards.firstOrNull { it.wardName == name }
                if (ward != null) viewModel.selectWard(ward)
            },
        )
        Spacer(Modifier.height(10.dp))

        // 사원번호
        OutlinedTextField(
            value = state.employeeNumber,
            onValueChange = viewModel::setEmployeeNumber,
            placeholder = { Text("사원번호") },
            leadingIcon = { Icon(Icons.Outlined.Badge, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = textFieldColors(),
        )
        Spacer(Modifier.height(10.dp))

        // 비밀번호
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::setPassword,
            placeholder = { Text("비밀번호") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = textFieldColors(),
        )

        // 에러 메시지
        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.error!!, fontSize = 13.sp, color = HnColors.Danger)
        }

        Spacer(Modifier.height(20.dp))
        HnButton(
            text = "로그인",
            full = true,
            enabled = valid && !state.loading,
            loading = state.loading,
            onClick = viewModel::login,
        )

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(HnColors.Border))
            Text("  또는 생체 인증으로 로그인  ", fontSize = 12.sp, color = HnColors.TextTertiary)
            Box(Modifier.weight(1f).height(1.dp).background(HnColors.Border))
        }
        Spacer(Modifier.height(16.dp))
        Row {
            HnButton(
                text = "지문 인증", icon = Icons.Outlined.Fingerprint,
                variant = HnButtonVariant.SECONDARY, full = true,
                onClick = viewModel::loginWithBiometric,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(10.dp))
            HnButton(
                text = "Face ID", icon = Icons.Outlined.Face,
                variant = HnButtonVariant.SECONDARY, full = true,
                onClick = viewModel::loginWithBiometric,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SelectField(
    icon: ImageVector,
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HnColors.Surface)
                .border(1.5.dp, if (open) HnColors.Primary else HnColors.Border, RoundedCornerShape(12.dp))
                .clickable(enabled = options.isNotEmpty()) { open = !open }
                .padding(horizontal = 16.dp),
        ) {
            Icon(icon, null, tint = if (open) HnColors.Primary else HnColors.TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(10.dp))
            Text(
                if (value.isBlank()) label else value,
                fontSize = 15.sp,
                fontWeight = if (value.isBlank()) FontWeight.Normal else FontWeight.Medium,
                color = if (value.isBlank()) HnColors.TextTertiary else HnColors.Text,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Outlined.ExpandMore, null, tint = HnColors.TextSecondary, modifier = Modifier.size(16.dp))
        }
        if (open && options.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HnColors.Surface)
                    .border(1.dp, HnColors.Border, RoundedCornerShape(12.dp)),
            ) {
                Column {
                    options.forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(opt); open = false }
                                .background(if (opt == value) HnColors.PrimarySoft else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                opt,
                                fontSize = 14.sp,
                                color = if (opt == value) HnColors.Primary else HnColors.Text,
                                fontWeight = if (opt == value) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = HnColors.Primary,
    unfocusedBorderColor = HnColors.Border,
    focusedLeadingIconColor = HnColors.Primary,
    unfocusedLeadingIconColor = HnColors.TextTertiary,
)
