import {ref} from 'vue'
import { defineStore } from 'pinia'

export const useNigthModeStore = defineStore('nightmode', () => {
    const nightMode = ref(false) 
    return { nightMode }
})
