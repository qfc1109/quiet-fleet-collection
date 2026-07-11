export function filterProjectsByName<T extends { name: string }>(projects: T[], keyword: string): T[] {
  const normalizedKeyword = keyword.trim().toLocaleLowerCase()
  if (!normalizedKeyword) {
    return projects
  }
  return projects.filter((project) => project.name.toLocaleLowerCase().includes(normalizedKeyword))
}
