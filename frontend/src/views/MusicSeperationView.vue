<template>
    <div>
        <div v-if="!store.check">
            <MusicSeperationMain/>
        </div>
        <div v-else>
            <MusicSeperationResult/>
        </div>
    </div>
</template>

<script setup lang="ts">
import MusicSeperationMain from '@/components/music-seperation/MusicSeperationMain.vue';
import MusicSeperationResult from '@/components/music-seperation/MusicSeperationResult.vue';
import { useSeperateStore } from '@/stores/seperate'

const store = useSeperateStore()
</script>
