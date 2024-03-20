import { ref } from 'vue'

import { defineStore } from 'pinia'

export const useLoadingAlarmStore = defineStore('loadingalram', () => {
    const loadingAlram = ref(false)
    return { loadingAlram }
})