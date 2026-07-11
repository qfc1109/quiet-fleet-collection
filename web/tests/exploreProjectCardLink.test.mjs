import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const exploreViewSource = readFileSync(resolve('src/views/ExploreView.vue'), 'utf8')
const styleSource = readFileSync(resolve('src/style.css'), 'utf8')

assert.match(exploreViewSource, /<RouterLink[\s\S]*v-for="project in filteredProjects"[\s\S]*class="project-card"/)
assert.match(exploreViewSource, /const searchKeyword = ref\(''\)/)
assert.match(exploreViewSource, /const filteredProjects = computed/)
assert.match(exploreViewSource, /v-model="searchKeyword"/)
assert.match(exploreViewSource, /v-else-if="filteredProjects\.length === 0"/)
assert.match(exploreViewSource, /aria-label="`打开项目 \$\{project\.name\}`"/)
assert.match(exploreViewSource, /class="project-card-action"/)
assert.doesNotMatch(exploreViewSource, /<article[^>]*class="project-card"/)
assert.match(styleSource, /\.project-card-action/)
assert.match(styleSource, /\.explore-search/)

console.log('Explore project card link checks passed.')
