<template>
    <div>
        <div v-if="!store.check">
            <MusicInputMain/>
        </div>
        <div v-else>
            <MusicInputComplete/>
        </div>
    </div>
</template>

<script setup lang="ts">
import MusicInputMain from '@/components/music-input/MusicInputMain.vue';
import MusicInputComplete from '@/components/music-input/MusicInputComplete.vue';
import { useMusicInputStore } from '@/stores/musicinput'

const store = useMusicInputStore()
</script>
