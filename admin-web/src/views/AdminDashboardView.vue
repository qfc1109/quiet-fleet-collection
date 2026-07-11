<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Edit, Key, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdminUser,
  createSiteUser,
  getAdminUsers,
  getPermissions,
  getRoles,
  getSiteFeedback,
  getSiteUsers,
  resetAdminUserPassword,
  resetSiteUserPassword,
  updateAdminUser,
  updateRole,
  updateSiteUser,
  type CreateAdminUserForm,
  type CreateUserForm,
  type UpdateAdminUserForm,
  type UpdateRoleForm,
  type UpdateUserForm,
} from '../api/adminManagement'
import type { AdminRoleView, PermissionView, SiteFeedbackView, UserAccountView } from '../api/types'
import { useSessionStore } from '../stores/session'

type AdminTab = 'site-users' | 'admin-users' | 'roles' | 'feedback'

const props = defineProps<{
  section: AdminTab
}>()

interface SiteUserForm extends CreateUserForm {
  id: number | null
  status: string
}

interface AdminUserForm extends CreateAdminUserForm {
  id: number | null
  status: string
}

interface RoleForm {
  id: number | null
  code: string
  name: string
  description: string
  permissionCodes: string[]
}

const session = useSessionStore()
const loading = ref(true)
const saving = ref(false)
const loadError = ref('')
const siteUsers = ref<UserAccountView[]>([])
const adminUsers = ref<UserAccountView[]>([])
const roles = ref<AdminRoleView[]>([])
const permissions = ref<PermissionView[]>([])
const feedbackList = ref<SiteFeedbackView[]>([])
const siteUserDialogOpen = ref(false)
const adminUserDialogOpen = ref(false)

const siteForm = ref<SiteUserForm>(emptySiteUserForm())
const adminForm = ref<AdminUserForm>(emptyAdminUserForm())
const roleForm = ref<RoleForm>(emptyRoleForm())

const editingSiteUser = computed(() => siteForm.value.id !== null)
const editingAdminUser = computed(() => adminForm.value.id !== null)
const editingRole = computed(() => roleForm.value.id !== null)
const selectedRoleIsSuperAdmin = computed(() => roleForm.value.code === 'SUPER_ADMIN')
const permissionGroups = computed(() => {
  const groups: Record<string, PermissionView[]> = {}
  for (const permission of permissions.value) {
    const moduleName = permission.module || '其他'
    groups[moduleName] = groups[moduleName] || []
    groups[moduleName].push(permission)
  }
  return Object.entries(groups).map(([moduleName, items]) => ({ moduleName, items }))
})

function emptySiteUserForm(): SiteUserForm {
  return {
    id: null,
    username: '',
    password: '',
    displayName: '',
    bio: '',
    avatarUrl: '',
    status: 'ENABLED',
  }
}

function emptyAdminUserForm(): AdminUserForm {
  return {
    id: null,
    username: '',
    password: '',
    displayName: '',
    bio: '',
    avatarUrl: '',
    roleCodes: ['NORMAL_ADMIN'],
    status: 'ENABLED',
  }
}

function emptyRoleForm(): RoleForm {
  return {
    id: null,
    code: '',
    name: '',
    description: '',
    permissionCodes: [],
  }
}

async function loadAdminData() {
  loading.value = true
  loadError.value = ''
  try {
    const [siteUserList, adminUserList, roleList, permissionList, siteFeedbackList] = await Promise.all([
      session.canViewUsers ? getSiteUsers() : Promise.resolve([]),
      session.canManageRoles ? getAdminUsers() : Promise.resolve([]),
      session.canManageRoles ? getRoles() : Promise.resolve([]),
      session.canManageRoles ? getPermissions() : Promise.resolve([]),
      session.canManageIssues ? getSiteFeedback() : Promise.resolve([]),
    ])
    siteUsers.value = siteUserList
    adminUsers.value = adminUserList
    roles.value = roleList
    permissions.value = permissionList
    feedbackList.value = siteFeedbackList
    if (props.section === 'roles' && roleList.length > 0 && !roleForm.value.id) {
      selectRole(roleList[0])
    }
  } catch (caught) {
    loadError.value = caught instanceof Error ? caught.message : '后台数据加载失败'
  } finally {
    loading.value = false
  }
}

function createNewSiteUser() {
  siteForm.value = emptySiteUserForm()
  siteUserDialogOpen.value = true
}

function selectSiteUser(user: UserAccountView) {
  siteForm.value = {
    id: user.id,
    username: user.username,
    password: '',
    displayName: user.displayName,
    bio: user.bio || '',
    avatarUrl: user.avatarUrl || '',
    status: user.status || 'ENABLED',
  }
  siteUserDialogOpen.value = true
}

function resetSiteUserForm() {
  siteForm.value = emptySiteUserForm()
}

async function saveSiteUser() {
  saving.value = true
  try {
    if (editingSiteUser.value && siteForm.value.id) {
      const form: UpdateUserForm = {
        displayName: siteForm.value.displayName,
        bio: siteForm.value.bio,
        avatarUrl: siteForm.value.avatarUrl,
        status: siteForm.value.status,
      }
      const updated = await updateSiteUser(siteForm.value.id, form)
      siteUsers.value = siteUsers.value.map((user) => (user.id === updated.id ? updated : user))
      siteUserDialogOpen.value = false
      ElMessage.success('网站用户已保存')
    } else {
      const created = await createSiteUser(siteForm.value)
      siteUsers.value = [created, ...siteUsers.value]
      siteUserDialogOpen.value = false
      ElMessage.success('网站用户已创建')
    }
  } catch (caught) {
    ElMessage.error(caught instanceof Error ? caught.message : '网站用户保存失败')
  } finally {
    saving.value = false
  }
}

async function resetSitePassword(user: UserAccountView) {
  try {
    const result = await ElMessageBox.prompt(`请输入 ${user.username} 的新密码`, '重置网站用户密码', {
      confirmButtonText: '确认重置',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPattern: /^.{6,}$/,
      inputErrorMessage: '密码至少 6 位',
    })
    await resetSiteUserPassword(user.id, String(result.value))
    ElMessage.success('网站用户密码已重置')
  } catch (caught) {
    if (caught !== 'cancel' && caught !== 'close') {
      ElMessage.error(caught instanceof Error ? caught.message : '密码重置失败')
    }
  }
}

function createNewAdminUser() {
  adminForm.value = emptyAdminUserForm()
  adminUserDialogOpen.value = true
}

function selectAdminUser(user: UserAccountView) {
  adminForm.value = {
    id: user.id,
    username: user.username,
    password: '',
    displayName: user.displayName,
    bio: user.bio || '',
    avatarUrl: user.avatarUrl || '',
    roleCodes: [...(user.roleCodes || [])],
    status: user.status || 'ENABLED',
  }
  adminUserDialogOpen.value = true
}

function resetAdminUserForm() {
  adminForm.value = emptyAdminUserForm()
}

async function saveAdminUser() {
  saving.value = true
  try {
    if (editingAdminUser.value && adminForm.value.id) {
      const form: UpdateAdminUserForm = {
        displayName: adminForm.value.displayName,
        bio: adminForm.value.bio,
        avatarUrl: adminForm.value.avatarUrl,
        status: adminForm.value.status,
        roleCodes: adminForm.value.roleCodes,
      }
      const updated = await updateAdminUser(adminForm.value.id, form)
      adminUsers.value = adminUsers.value.map((user) => (user.id === updated.id ? updated : user))
      adminUserDialogOpen.value = false
      ElMessage.success('后台管理员已保存')
    } else {
      const created = await createAdminUser(adminForm.value)
      adminUsers.value = [created, ...adminUsers.value]
      adminUserDialogOpen.value = false
      ElMessage.success('后台管理员已创建')
    }
  } catch (caught) {
    ElMessage.error(caught instanceof Error ? caught.message : '后台管理员保存失败')
  } finally {
    saving.value = false
  }
}

async function resetAdminPassword(user: UserAccountView) {
  try {
    const result = await ElMessageBox.prompt(`请输入 ${user.username} 的新密码`, '重置后台管理员密码', {
      confirmButtonText: '确认重置',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPattern: /^.{6,}$/,
      inputErrorMessage: '密码至少 6 位',
    })
    await resetAdminUserPassword(user.id, String(result.value))
    ElMessage.success('后台管理员密码已重置')
  } catch (caught) {
    if (caught !== 'cancel' && caught !== 'close') {
      ElMessage.error(caught instanceof Error ? caught.message : '密码重置失败')
    }
  }
}

function selectRole(role: AdminRoleView) {
  roleForm.value = {
    id: role.id,
    code: role.code,
    name: role.name,
    description: role.description || '',
    permissionCodes: [...(role.permissionCodes || [])],
  }
}

async function saveRole() {
  if (!roleForm.value.id || selectedRoleIsSuperAdmin.value) {
    return
  }
  saving.value = true
  try {
    const form: UpdateRoleForm = {
      name: roleForm.value.name,
      description: roleForm.value.description,
      permissionCodes: roleForm.value.permissionCodes,
    }
    const updated = await updateRole(roleForm.value.id, form)
    roles.value = roles.value.map((role) => (role.id === updated.id ? updated : role))
    selectRole(updated)
    ElMessage.success('角色权限已保存')
  } catch (caught) {
    ElMessage.error(caught instanceof Error ? caught.message : '角色保存失败')
  } finally {
    saving.value = false
  }
}

function statusText(status: string) {
  return status === 'DISABLED' ? '停用' : '启用'
}

function statusTagType(status: string) {
  return status === 'DISABLED' ? 'danger' : 'success'
}

function feedbackStatusText(status: string) {
  return status === 'OPEN' ? '待处理' : status
}

function feedbackAuthorName(row: SiteFeedbackView) {
  return row.authorDisplayName || row.authorUsername || `用户 #${row.authorUserId}`
}

function formatDate(value: string) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', {
    hour12: false,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function roleNames(codes: string[] = []) {
  return codes
    .map((code) => roles.value.find((role) => role.code === code)?.name || code)
    .join('、')
}

onMounted(loadAdminData)
</script>

<template>
  <section v-loading="loading" class="admin-page">
    <el-alert v-if="loadError" :title="loadError" type="error" show-icon :closable="false" />

    <template v-else>
      <el-card v-if="props.section === 'site-users'" class="management-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h2>用户列表</h2>
              <p>{{ siteUsers.length }} 个网站用户</p>
            </div>
            <el-button type="primary" :icon="Plus" @click="createNewSiteUser">新增用户</el-button>
          </div>
        </template>

        <el-table class="manager-table" :data="siteUsers" border stripe row-key="id">
          <el-table-column label="用户" min-width="220">
            <template #default="{ row }">
              <div class="identity-cell">
                <strong>{{ row.displayName }}</strong>
                <span>{{ row.bio || '无简介' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="username" label="账号" min-width="160" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" min-width="180">
            <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-space>
                <el-button size="small" :icon="Edit" @click="selectSiteUser(row)">编辑</el-button>
                <el-button size="small" :icon="Key" @click="resetSitePassword(row)">重置密码</el-button>
              </el-space>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无网站用户" />
          </template>
        </el-table>

        <el-dialog
          v-model="siteUserDialogOpen"
          :title="editingSiteUser ? '编辑网站用户' : '新增网站用户'"
          width="560px"
          :close-on-click-modal="!saving"
          @closed="resetSiteUserForm"
        >
          <el-form :model="siteForm" label-position="top">
            <el-form-item label="账号" required>
              <el-input v-model="siteForm.username" :disabled="editingSiteUser" />
            </el-form-item>
            <el-form-item v-if="!editingSiteUser" label="初始密码" required>
              <el-input v-model="siteForm.password" type="password" show-password />
            </el-form-item>
            <el-form-item label="显示名" required>
              <el-input v-model="siteForm.displayName" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="siteForm.status" class="full-control">
                <el-option label="启用" value="ENABLED" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item label="简介">
              <el-input v-model="siteForm.bio" type="textarea" :rows="4" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="siteUserDialogOpen = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="saveSiteUser">保存</el-button>
          </template>
        </el-dialog>
      </el-card>

      <el-card v-else-if="props.section === 'admin-users'" class="management-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h2>管理员列表</h2>
              <p>{{ adminUsers.length }} 个后台账号</p>
            </div>
            <el-button type="primary" :icon="Plus" @click="createNewAdminUser">新增管理员</el-button>
          </div>
        </template>

        <el-table class="manager-table" :data="adminUsers" border stripe row-key="id">
          <el-table-column label="管理员" min-width="220">
            <template #default="{ row }">
              <div class="identity-cell">
                <strong>{{ row.displayName }}</strong>
                <span>{{ row.bio || '无简介' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="username" label="账号" min-width="150" />
          <el-table-column label="角色" min-width="180">
            <template #default="{ row }">{{ roleNames(row.roleCodes) || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" min-width="180">
            <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-space>
                <el-button size="small" :icon="Edit" @click="selectAdminUser(row)">编辑</el-button>
                <el-button size="small" :icon="Key" @click="resetAdminPassword(row)">重置密码</el-button>
              </el-space>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无后台管理员" />
          </template>
        </el-table>

        <el-dialog
          v-model="adminUserDialogOpen"
          :title="editingAdminUser ? '编辑后台管理员' : '新增后台管理员'"
          width="600px"
          :close-on-click-modal="!saving"
          @closed="resetAdminUserForm"
        >
          <el-form :model="adminForm" label-position="top">
            <el-form-item label="账号" required>
              <el-input v-model="adminForm.username" :disabled="editingAdminUser" />
            </el-form-item>
            <el-form-item v-if="!editingAdminUser" label="初始密码" required>
              <el-input v-model="adminForm.password" type="password" show-password />
            </el-form-item>
            <el-form-item label="显示名" required>
              <el-input v-model="adminForm.displayName" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="adminForm.status" class="full-control">
                <el-option label="启用" value="ENABLED" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item label="角色">
              <el-checkbox-group v-model="adminForm.roleCodes">
                <el-checkbox-button v-for="role in roles" :key="role.id" :value="role.code">
                  {{ role.name }}
                </el-checkbox-button>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="简介">
              <el-input v-model="adminForm.bio" type="textarea" :rows="4" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="adminUserDialogOpen = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="saveAdminUser">保存</el-button>
          </template>
        </el-dialog>
      </el-card>

      <el-card v-else-if="props.section === 'feedback'" class="management-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h2>主站反馈</h2>
              <p>{{ feedbackList.length }} 条体验反馈</p>
            </div>
          </div>
        </template>

        <el-table class="manager-table" :data="feedbackList" border stripe row-key="id">
          <el-table-column label="反馈内容" min-width="340">
            <template #default="{ row }">
              <div class="feedback-content-cell">
                <strong>{{ row.title }}</strong>
                <p>{{ row.content }}</p>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="提交用户" min-width="180">
            <template #default="{ row }">
              <div class="identity-cell">
                <strong>{{ feedbackAuthorName(row) }}</strong>
                <span>{{ row.authorUsername || `ID ${row.authorUserId}` }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag effect="light">{{ feedbackStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" min-width="180">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无主站反馈" />
          </template>
        </el-table>
      </el-card>

      <el-row v-else-if="props.section === 'roles'" :gutter="16" class="role-layout">
        <el-col :xs="24" :lg="8">
          <el-card class="management-card" shadow="never">
            <template #header>
              <div class="card-header">
                <div>
                  <h2>固定角色</h2>
                  <p>{{ roles.length }} 个角色</p>
                </div>
              </div>
            </template>
            <el-table :data="roles" border row-key="id" highlight-current-row @row-click="selectRole">
              <el-table-column prop="name" label="角色" min-width="140" />
              <el-table-column prop="code" label="编码" min-width="150" />
              <template #empty>
                <el-empty description="暂无角色" />
              </template>
            </el-table>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="16">
          <el-card class="management-card" shadow="never">
            <template #header>
              <div class="card-header">
                <div>
                  <h2>{{ editingRole ? '角色权限' : '选择角色' }}</h2>
                  <p>{{ selectedRoleIsSuperAdmin ? '超级管理员默认拥有全部权限' : roleForm.code || '请选择一个角色' }}</p>
                </div>
                <el-button
                  v-if="editingRole && !selectedRoleIsSuperAdmin"
                  type="primary"
                  :loading="saving"
                  @click="saveRole"
                >
                  保存权限
                </el-button>
              </div>
            </template>

            <el-empty v-if="!editingRole" description="请选择一个角色查看权限" />
            <el-form v-else :model="roleForm" label-position="top">
              <el-row :gutter="16">
                <el-col :xs="24" :md="12">
                  <el-form-item label="角色编码">
                    <el-input v-model="roleForm.code" disabled />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="12">
                  <el-form-item label="角色名称">
                    <el-input v-model="roleForm.name" :disabled="selectedRoleIsSuperAdmin" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="说明">
                <el-input v-model="roleForm.description" :disabled="selectedRoleIsSuperAdmin" type="textarea" :rows="3" />
              </el-form-item>

              <div class="permission-groups">
                <el-card v-for="group in permissionGroups" :key="group.moduleName" shadow="never" class="permission-card">
                  <template #header>{{ group.moduleName }}</template>
                  <el-checkbox-group v-model="roleForm.permissionCodes" :disabled="selectedRoleIsSuperAdmin">
                    <el-checkbox v-for="permission in group.items" :key="permission.id" :value="permission.code" border>
                      <span class="permission-name">{{ permission.name }}</span>
                      <small>{{ permission.code }}</small>
                    </el-checkbox>
                  </el-checkbox-group>
                </el-card>
              </div>
            </el-form>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </section>
</template>
