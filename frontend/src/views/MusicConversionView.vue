<template>
    <div>
        <div v-if="!store.check">
            <MusicConversionMain/>
        </div>
        <div v-else>
            <MusicConversionResult/>
        </div>
    </div>
</template>

<script setup lang="ts">
import MusicConversionResult from '@/components/music-conversion/MusicConversionResult.vue';
import MusicConversionMain from '@/components/music-conversion/MusicConversionMain.vue';
import { useSeperateStore } from '@/stores/seperate'

const store = useSeperateStore()
</script>
