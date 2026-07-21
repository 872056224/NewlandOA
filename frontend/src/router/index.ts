import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/emp-login'
    },
    {
      path: '/emp-login',
      name: 'EmpLogin',
      component: () => import('../components/emp/EmpLogin.vue')
    },
    {
      path: '/emp-home',
      name: 'EmpHome',
      component: () => import('../components/emp/EmpHome.vue'),
      children: [
        {
          path: 'info',
          name: 'EmpInfo',
          component: () => import('../components/emp/EmpInfo.vue')
        },
        {
          path: 'sign-in',
          name: 'EmpSignIn',
          component: () => import('../components/emp/EmpSignIn.vue')
        },
        {
          path: 'sign-message',
          name: 'EmpSignMessage',
          component: () => import('../components/emp/EmpSignMessage.vue')
        },
        {
          path: 'update-pwd',
          name: 'EmpUpdatePwd',
          component: () => import('../components/emp/EmpUpdatePwd.vue')
        },
        {
          path: 'ai-chat',
          name: 'EmpAiChat',
          component: () => import('../components/emp/EmpAiChat.vue')
        },
        {
          path: 'leave-apply',
          name: 'EmpLeaveApply',
          component: () => import('../components/emp/EmpLeaveApply.vue')
        },
        {
          path: 'leave-list',
          name: 'EmpLeaveList',
          component: () => import('../components/emp/EmpLeaveList.vue')
        }
      ]
    },
    {
      path: '/admin-login',
      name: 'AdminLogin',
      component: () => import('../components/admin/AdminLogin.vue')
    },
    {
      path: '/admin-home',
      name: 'AdminHome',
      component: () => import('../components/admin/AdminHome.vue'),
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('../components/admin/Dashboard.vue')
        },
        {
          path: 'emp-list',
          name: 'EmpList',
          component: () => import('../components/admin/EmpList.vue')
        },
        {
          path: 'dept-manage',
          name: 'DeptManage',
          component: () => import('../components/admin/DeptManage.vue')
        },
        {
          path: 'duty-manage',
          name: 'DutyManage',
          component: () => import('../components/admin/DutyManage.vue')
        },
        {
          path: 'sign-list',
          name: 'SignList',
          component: () => import('../components/admin/SignList.vue')
        },
        {
          path: 'sign-statistics',
          name: 'SignStatistics',
          component: () => import('../components/admin/SignStatistics.vue')
        },
        {
          path: 'kb-manage',
          name: 'KbManage',
          component: () => import('../components/admin/KbManage.vue')
        },
        {
          path: 'signed-list',
          name: 'SignedList',
          component: () => import('../components/admin/SignedList.vue')
        },
        {
          path: 'unsigned-list',
          name: 'UnsignedList',
          component: () => import('../components/admin/UnsignedList.vue')
        },
        {
          path: 'retroactive-approval',
          name: 'RetroactiveApproval',
          component: () => import('../components/admin/RetroactiveApproval.vue')
        },
        {
          path: 'leave-approval',
          name: 'LeaveApproval',
          component: () => import('../components/admin/LeaveApproval.vue')
        }
      ]
    }
  ]
})

export default router
