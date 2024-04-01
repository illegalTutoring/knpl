<template>
    <div>
        <div v-if="!store.check">
            <MusicSeparationMain />
        </div>
        <div v-else>
            <MusicSeparationResult />
        </div>
    </div>
</template>

<script setup lang="ts">
import MusicSeparationMain from "@/components/music-separation/MusicSeparationMain.vue";
import MusicSeparationResult from "@/components/music-separation/MusicSeparationResult.vue";
import { useSeparateStore } from "@/stores/separate";

const store = useSeparateStore();
</script>
