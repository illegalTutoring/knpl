import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import axios from "axios";

export const useMusicInputStore = defineStore('musicinput', () => {
    const check = ref(false)
    return {check}
})