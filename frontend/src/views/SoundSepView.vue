<template>
    <div>
        <div v-if="!store.check">
            <SepSoundtitle/>
        </div>
        <div v-else>
            <SepSound/>
        </div>
    </div>
</template>

<script setup lang="ts">
import SepSoundtitle from '@/components/sepviewcomponents/SepSoundtitle.vue';
import SepSound from '@/components/sepviewcomponents/SepSound.vue';
import { useSeperateStore } from '@/stores/seperate'

const store = useSeperateStore()
</script>
