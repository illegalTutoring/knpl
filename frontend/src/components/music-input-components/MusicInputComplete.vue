<template>
    <div>
        완성됐습니다!
        <button @click="back"> 완성됐습니다.</button>
    </div>
</template>

<script setup lang="ts">
import { useSeperateStore } from '@/stores/seperate'

const store = useSeperateStore()

const back = () => {
    store.check = false
}

</script>

<style scoped>

</style>