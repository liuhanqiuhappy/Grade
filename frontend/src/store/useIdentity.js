import { defineStore } from 'pinia'

export const useIdentityStore = defineStore('identity',{
  state: ()=>({
    userId: null,
    username: "",
    displayName: "",
    role: "" // TEACHER / STUDENT
  }),
  actions:{
    setUser(info){
      this.userId = info.userId
      this.username = info.username
      this.displayName = info.displayName
      this.role = info.role
    },
    clearUser(){
      this.userId = null
      this.username = ""
      this.displayName = ""
      this.role = ""
    }
  }
})
