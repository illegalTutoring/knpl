<script setup lang="ts">
import { RouterView } from "vue-router";
import Navbar from "@/components/navbar/Navbar.vue";
import "@/css/global.css";
</script>

<template>
  <div>
    <div>
      <Navbar />
      <RouterView />
    </div>
  </div>
</template>
