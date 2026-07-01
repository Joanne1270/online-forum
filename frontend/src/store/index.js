import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

const saved = localStorage.getItem('forum_user')
const token = localStorage.getItem('forum_token')

export default new Vuex.Store({
  state: {
    token: token || '',
    user: saved ? JSON.parse(saved) : null
  },
  getters: {
    isLogin: state => !!state.token,
    isAdmin: state => state.user && state.user.role === 'ADMIN',
    user: state => state.user || {}
  },
  mutations: {
    SET_AUTH(state, { token, user }) {
      state.token = token
      state.user = user
      localStorage.setItem('forum_token', token)
      localStorage.setItem('forum_user', JSON.stringify(user))
    },
    CLEAR_AUTH(state) {
      state.token = ''
      state.user = null
      localStorage.removeItem('forum_token')
      localStorage.removeItem('forum_user')
    }
  },
  actions: {
    login({ commit }, payload) {
      commit('SET_AUTH', payload)
    },
    logout({ commit }) {
      commit('CLEAR_AUTH')
    }
  }
})
