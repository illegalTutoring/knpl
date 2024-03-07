<template>
    <div v-if="!store.check">
        <MusicInputTitle/>
    </div>
    <div v-else>
        <MusicInputComplete/>
    </div>
</template>

<script setup lang="ts">
import MusicInputTitle from '@/components/music-input-components/MusicInputTitle.vue';
import MusicInputComplete from '@/components/music-input-components/MusicInputComplete.vue';
import { useSeperateStore } from '@/stores/seperate'

const store = useSeperateStore()
</script>
