<script setup lang="ts">
import { ref } from 'vue'
import { RouterView } from 'vue-router'
import navbar from './components/basicscreennavbar/navbar.vue';

</script>

<template>
    <div>
        <div>
            <navbar />
        </div>

        <div>
            <RouterView />
        </div>
    </div>
</template>

