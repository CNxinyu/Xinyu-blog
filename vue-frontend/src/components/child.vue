<template>
    <div>
        紫薯{{msgCount}}
         {{ data }}
    </div>
</template>
<script setup>
import { computed } from 'vue';
const props = defineProps({
    data: {
        type: String,
        required: true
    }
});
const msgCount = computed(() => {
    return props.data.length;
});
// defineProps(['data'])
</script>