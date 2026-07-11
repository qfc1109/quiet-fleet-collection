import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const rootDir = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const sources = {
  spaceView: readFileSync(resolve(rootDir, 'src/views/SpaceView.vue'), 'utf8'),
  fileTreeList: readFileSync(resolve(rootDir, 'src/components/FileTreeList.vue'), 'utf8'),
  fileTree: readFileSync(resolve(rootDir, 'src/utils/fileTree.ts'), 'utf8'),
}

const checks = [
  {
    message: 'tracks the active space module in component state',
    test: () => /const activeSpaceModule = ref<SpaceModule>\('account'\)/.test(sources.spaceView),
  },
  {
    message: 'renders account center and project management in a left module sidebar',
    test: () => /<aside class="space-module-sidebar"[\s\S]*账号中心[\s\S]*项目管理[\s\S]*<\/aside>/.test(sources.spaceView),
  },
  {
    message: 'shows account profile only when the account module is active',
    test: () =>
      /<section v-if="activeSpaceModule === 'account'" class="space-module-content account-profile-section"/.test(
        sources.spaceView,
      ),
  },
  {
    message: 'shows project management only when the projects module is active',
    test: () => /<template v-else-if="activeSpaceModule === 'projects'">/.test(sources.spaceView),
  },
  {
    message: 'shows the create project action only inside project management',
    test: () => /v-if="activeSpaceModule === 'projects' && hasProjects"/.test(sources.spaceView),
  },
  {
    message: 'renames the project slug field to a human-readable access path label',
    test: () => /<span>访问路径<\/span>/.test(sources.spaceView) && !/<span>slug<\/span>/.test(sources.spaceView),
  },
  {
    message: 'adds an access path hint for project slug inputs',
    test: () => /class="field-hint"[\s\S]*\/p\//.test(sources.spaceView),
  },
  {
    message: 'carries created time on virtual directory tree rows',
    test: () => /type: 'directory'[\s\S]*createdAt: string[\s\S]*child\.createdAt/.test(sources.fileTree),
  },
  {
    message: 'renders added time for directory rows',
    test: () => /formatFileAddedTime\(row\.createdAt\)/.test(sources.fileTreeList),
  },
  {
    message: 'renders added time for file rows',
    test: () => /formatFileAddedTime\(row\.file\.createdAt\)/.test(sources.fileTreeList),
  },
]

const failures = checks.filter((check) => !check.test())

if (failures.length > 0) {
  console.error('SpaceView module structure checks failed:')
  for (const failure of failures) {
    console.error(`- ${failure.message}`)
  }
  process.exit(1)
}

console.log('SpaceView module structure checks passed.')
