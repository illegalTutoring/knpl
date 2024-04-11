import { ref } from 'vue';

import { defineStore } from 'pinia';

export const useLoadingAlarmStore = defineStore('loadingalram', () => {
    const loadingAlarm = ref(false);
    return { loadingAlarm };
});
