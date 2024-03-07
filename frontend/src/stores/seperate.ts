import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useSeperateStore = defineStore('counter', () => {
    const check = ref(false) 
  
    return { check, }
})