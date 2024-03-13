<template>
    <div v-if="!store.check">
        <MusicInputMain/>
    </div>
    <div v-else>
        <MusicInputComplete/>
    </div>
</template>

<script setup lang="ts">
import MusicInputMain from '@/components/music-input/MusicInputMain.vue';
import MusicInputComplete from '@/components/music-input/MusicInputComplete.vue';
import { useSeperateStore } from '@/stores/seperate'

const store = useSeperateStore()
</script>
