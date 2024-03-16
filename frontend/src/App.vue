<script setup lang="ts">
import { RouterView } from 'vue-router'
import Navbar from '@/components/navbar-components/Navbar.vue';

</script>

<template>
    <div>
        <div>
            <Navbar />
        </div>

        <div>
            <RouterView />
        </div>
    </div>
</template>

