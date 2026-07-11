import assert from 'node:assert/strict'
import { filterProjectsByName } from '../src/utils/projectSearch.ts'

const projects = [
  { id: 1, name: '小画文档' },
  { id: 2, name: 'Battle Server Notes' },
  { id: 3, name: '运营配置手册' },
]

assert.deepEqual(
  filterProjectsByName(projects, '小画').map((project) => project.name),
  ['小画文档'],
)

assert.deepEqual(
  filterProjectsByName(projects, ' server ').map((project) => project.name),
  ['Battle Server Notes'],
)

assert.deepEqual(filterProjectsByName(projects, '').map((project) => project.name), [
  '小画文档',
  'Battle Server Notes',
  '运营配置手册',
])

assert.deepEqual(filterProjectsByName(projects, '不存在'), [])
