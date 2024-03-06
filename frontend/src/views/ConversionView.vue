<template>
    <div>
        <div v-if="!store.check">
            <conversiontitle/>
        </div>
        <div v-else>
            <conversion/>
        </div>
    </div>
</template>

<script setup lang="ts">
import conversiontitle from '@/components/conversioncomponents/conversiontitle.vue';
import conversion from '@/components/conversioncomponents/conversion.vue';
import { useSeperateStore } from '@/stores/seperate'

const store = useSeperateStore()
</script>
