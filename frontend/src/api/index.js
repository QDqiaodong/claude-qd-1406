import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error?.response?.data?.message || error?.message || '请求失败'
    return Promise.reject(new Error(message))
  }
)

async function call(promise) {
  const response = await promise
  const body = response.data
  if (!body || body.ok === false) {
    throw new Error((body && body.message) || '请求失败')
  }
  return body.data
}

const params = (o) => ({ params: o })

export default {
  // ---------------- 科室 dept ----------------
  listDepts: () => call(http.get('/depts')),
  listAvailableDepts: () => call(http.get('/depts/available')),
  createDept: (d) => call(http.post('/depts', d)),
  updateDept: (id, d) => call(http.put(`/depts/${id}`, d)),
  transferDept: (id, status) => call(http.post(`/depts/${id}/status`, null, params({ status }))),

  // ---------------- 体检套餐 package ----------------
  listPackages: () => call(http.get('/packages')),
  listBookablePackages: () => call(http.get('/packages/bookable')),
  createPackage: (d) => call(http.post('/packages', d)),
  updatePackage: (id, d) => call(http.put(`/packages/${id}`, d)),
  transferPackage: (id, status) => call(http.post(`/packages/${id}/status`, null, params({ status }))),

  // ---------------- 体检人 examinee ----------------
  listExaminees: (packageId) => call(http.get('/examinees', params(packageId ? { packageId } : {}))),
  createExaminee: (d) => call(http.post('/examinees', d)),
  updateExaminee: (id, d) => call(http.put(`/examinees/${id}`, d)),
  startExaminee: (id) => call(http.post(`/examinees/${id}/start`)),
  transferExaminee: (id, status) => call(http.post(`/examinees/${id}/status`, null, params({ status }))),

  // ---------------- 检查项记录 exam ----------------
  listExams: (examQuery) => call(http.get('/exams', params(examQuery || {}))),
  examProgress: (examineeId) => call(http.get('/exams/progress', params({ examineeId }))),
  createExam: (d) => call(http.post('/exams', d)),
  updateExam: (id, d) => call(http.put(`/exams/${id}`, d)),
  transferExam: (id, status, result, abnormal) =>
    call(http.post(`/exams/${id}/status`, null, params({ status, result, abnormal }))),
  deleteExam: (id) => call(http.delete(`/exams/${id}`))
}
