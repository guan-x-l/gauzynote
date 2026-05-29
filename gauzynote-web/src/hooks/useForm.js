import { ref, reactive, computed } from 'vue'

export function useForm(initialValues = {}) {
    const formData = reactive({ ...initialValues })
    const fields = ref({})

    // 注册表单项
    const registerField = (field) => {
        fields.value[field.name] = {
            ...field,
            valid: true,
            message: ''
        }

        // 如果初始值存在，设置到表单数据中
        if (formData[field.name] === undefined && field.defaultValue !== undefined) {
            formData[field.name] = field.defaultValue
        }
    }

    // 注销表单项
    const unregisterField = (name) => {
        delete fields.value[name]
    }

    // 校验单个字段
    const validateField = (name) => {
        const field = fields.value[name]
        if (!field) return Promise.resolve(true)

        return field.validate().then(valid => {
            fields.value[name] = {
                ...field,
                valid,
                message: valid ? '' : field.message
            }
            return valid
        })
    }

    // 校验所有字段
    const validateAllFields = () => {
        const fieldNames = Object.keys(fields.value)
        const promises = fieldNames.map(name => validateField(name))

        return Promise.all(promises).then(results => {
            return results.every(valid => valid)
        })
    }

    // 重置所有字段
    const resetFields = (values = {}) => {
        // 重置表单数据
        console.log(formData, initialValues, values)
        Object.assign(formData, initialValues, values)

        // 重置校验状态
        Object.keys(fields.value).forEach(name => {
            fields.value[name] = {
                ...fields.value[name],
                valid: true,
                message: ''
            }
        })
    }

    // 表单是否有效
    const isFormValid = computed(() => {
        return Object.values(fields.value).every(field => field.valid)
    })

    return {
        formData,
        fields,
        registerField,
        unregisterField,
        validateField,
        validateAllFields,
        resetFields,
        isFormValid
    }
}