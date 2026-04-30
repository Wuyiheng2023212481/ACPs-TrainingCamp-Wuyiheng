document.addEventListener('DOMContentLoaded', function() {
    // ==================== 全局变量和初始化 ====================
    const navLinks = document.querySelectorAll('.nav-link');
    const contentSections = document.querySelectorAll('.content-section');
    let currentSection = 'overview';

    // 系统状态
    const systemStatus = {
        adc_dac: true,
        coding: true,
        protocol: true,
        modulation: true,
        switching: true
    };

    // ==================== 通用功能 ====================
    // 初始化导航栏
    function initNavigation() {
        // 默认激活第一个导航项
        navLinks[0].classList.add('active');
        document.getElementById('overview').classList.add('active');

        navLinks.forEach(link => {
            link.addEventListener('click', function(e) {
                e.preventDefault();

                // 移除所有active类
                navLinks.forEach(l => l.classList.remove('active'));
                contentSections.forEach(section => section.classList.remove('active'));

                // 添加active类到当前点击的链接和对应的内容区域
                this.classList.add('active');
                const targetId = this.getAttribute('data-target');
                document.getElementById(targetId).classList.add('active');
                currentSection = targetId;

                // 根据当前显示的内容初始化相应的可视化
                initVisualization(targetId);
            });
        });
    }

    // 初始化可视化
    function initVisualization(targetId) {
        switch(targetId) {
            case 'adc_dac':
                initWaveformCharts();
                break;
            case 'modulation':
                initModulationCharts();
                break;
            case 'switching':
                updateSwitchingMetrics();
                break;
        }
    }

    // 更新时间戳
    function updateTimestamp() {
        const now = new Date();
        const timestamp = now.toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });

        document.querySelectorAll('.timestamp').forEach(el => {
            if (!el.id.includes('timestamp')) {
                el.textContent = timestamp;
            }
        });

        // 更新全局时间戳
        document.getElementById('global-timestamp').textContent = timestamp;
    }

    // 更新部分时间戳
    function updateSectionTimestamp(sectionId) {
        const now = new Date();
        const timestamp = now.toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });

        const timestampEl = document.getElementById(`${sectionId}-timestamp`);
        if (timestampEl) {
            timestampEl.textContent = timestamp;
        }
    }

    // 检查系统状态
    function checkSystemStatus() {
        fetch('/system_status')
            .then(response => response.json())
            .then(data => {
                // 更新系统状态显示
                Object.keys(systemStatus).forEach(key => {
                    const statusEl = document.querySelector(`.status-item:nth-child(${Object.keys(systemStatus).indexOf(key) + 1}) .status-value`);
                    if (statusEl) {
                        statusEl.textContent = '运行中';
                        statusEl.className = 'status-value active';
                    }
                });
            })
            .catch(error => {
                console.error('Error checking system status:', error);
                Object.keys(systemStatus).forEach(key => {
                    const statusEl = document.querySelector(`.status-item:nth-child(${Object.keys(systemStatus).indexOf(key) + 1}) .status-value`);
                    if (statusEl) {
                        statusEl.textContent = '异常';
                        statusEl.className = 'status-value error';
                    }
                });
            });
    }

    // 格式化二进制数据
    function formatBinaryData(binaryStr, groupSize) {
        if (!binaryStr) return '';

        let formatted = '';
        for (let i = 0; i < binaryStr.length; i += groupSize) {
            const chunk = binaryStr.substr(i, groupSize);
            formatted += chunk + ' ';
        }
        return formatted.trim();
    }

    // 动画数字效果
    function animateValue(element, start, end, duration) {
        const range = end - start;
        let current = start;
        const increment = end > start ? 1 : -1;
        const stepTime = Math.abs(Math.floor(duration / range));
        const timer = setInterval(() => {
            current += increment;
            element.textContent = current;
            if (current === end) {
                clearInterval(timer);
            }
        }, stepTime);
    }

    // ==================== 音频信号处理 ====================
    function initAudioProcessing() {
        const processBtn = document.getElementById('processAudioBtn');

        processBtn.addEventListener('click', function() {
            processBtn.disabled = true;
            processBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 处理中...';

            fetch('/process_audio_signal')
                .then(response => response.json())
                .then(data => {
                    // 绘制原始音频波形
                    drawWaveform('audioWave', data.time, data.analog_signal, '#3498db', '原始音频');

                    // 绘制采样波形
                    drawSampledWaveform('audioSampled', data.time, data.analog_signal,
                                      data.sample_points, data.sampled_signal, data.quantized_signal);

                    // 显示编码数据
                    document.getElementById('audioEncoded').textContent =
                        formatBinaryData(data.encoded, 8).substring(0, 100) + '...';

                    // 显示TCP握手结果
                    const handshakeDiv = document.getElementById('audioHandshake');
                    if (data.handshake_result.status === 'success') {
                        handshakeDiv.innerHTML = `
                            <div class="alert alert-success">
                                <p><i class="fas fa-check-circle"></i> 握手成功</p>
                                <p>连接ID: ${data.handshake_result.connection_id}</p>
                            </div>
                        `;
                    } else {
                        handshakeDiv.innerHTML = `
                            <div class="alert alert-error">
                                <p><i class="fas fa-exclamation-triangle"></i> 握手失败</p>
                                <p>原因: ${data.handshake_result.reason}</p>
                            </div>
                        `;
                    }

                    // 绘制调制波形
                    const time = data.time.slice(0, data.modulated.length);
                    drawAnalogSignal('audioModulated', time, data.modulated, '#9b59b6', 'QPSK调制');

                    // 显示路由信息
                    document.getElementById('audioRoute').innerHTML = `
                        <p>目标: ${data.route.destination}</p>
                        <p>延迟: ${data.route.latency}ms</p>
                        <p>容量: ${data.route.capacity}Mbps</p>
                    `;

                    // 显示波束信息
                    document.getElementById('audioBeam').innerHTML = `
                        <p>角度: ${data.beam_angle}°</p>
                        <div class="beam-visual">
                            <div class="beam-line" style="transform: rotate(${data.beam_angle}deg)"></div>
                        </div>
                    `;

                    // 更新时间戳
                    document.getElementById('audio-timestamp').textContent =
                        new Date(data.timestamp).toLocaleString();

                    processBtn.disabled = false;
                    processBtn.innerHTML = '<i class="fas fa-sync-alt"></i> 重新处理';
                })
                .catch(error => {
                    console.error('Error processing audio:', error);
                    processBtn.disabled = false;
                    processBtn.innerHTML = '<i class="fas fa-play"></i> 开始处理';
                });
        });
    }

    // ==================== 子课题一：AD/DA转换 ====================
    function initADDAConversion() {
        const signalTypeSelect = document.getElementById('signal-type');
        const frequencySlider = document.getElementById('frequency');
        const frequencyValue = document.getElementById('frequency-value');
        const amplitudeSlider = document.getElementById('amplitude');
        const amplitudeValue = document.getElementById('amplitude-value');
        const sampleRateSlider = document.getElementById('sample-rate');
        const sampleRateValue = document.getElementById('sample-rate-value');
        const bitDepthSelect = document.getElementById('bit-depth');
        const updateBtn = document.getElementById('update-btn');

        // 初始化滑块显示值
        frequencyValue.textContent = parseFloat(frequencySlider.value).toFixed(1);
        amplitudeValue.textContent = amplitudeSlider.value;
        sampleRateValue.textContent = sampleRateSlider.value;

        // 滑块事件监听
        frequencySlider.addEventListener('input', function() {
            frequencyValue.textContent = parseFloat(this.value).toFixed(1);
        });

        amplitudeSlider.addEventListener('input', function() {
            amplitudeValue.textContent = this.value;
        });

        sampleRateSlider.addEventListener('input', function() {
            sampleRateValue.textContent = this.value;
        });

        // 更新按钮事件
        updateBtn.addEventListener('click', function() {
            updateWaveforms();
        });

        // 初始加载波形
        updateWaveforms();
    }

    function updateWaveforms() {
        const config = {
            signal_type: document.getElementById('signal-type').value,
            frequency: parseFloat(document.getElementById('frequency').value),
            amplitude: parseFloat(document.getElementById('amplitude').value),
            sample_rate: parseInt(document.getElementById('sample-rate').value),
            bit_depth: parseInt(document.getElementById('bit-depth').value)
        };

        fetch('/get_waveform', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(config)
        })
        .then(response => response.json())
        .then(data => {
            // 更新性能指标
            document.getElementById('metric-sample-rate').textContent = data.config.sample_rate + ' kHz';
            document.getElementById('metric-bit-depth').textContent = data.config.bit_depth + ' bit';
            document.getElementById('metric-snr').textContent = data.snr.toFixed(2) + ' dB';
            document.getElementById('metric-nyquist').textContent = (data.config.sample_rate / 2) + ' kHz';

            // 绘制波形
            drawWaveform('analogWave', data.time, data.analog_signal, '#3498db', '原始模拟信号');
            drawSampledWaveform('sampledWave', data.time, data.analog_signal, data.sample_points, data.sampled_signal, data.quantized_signal);
            drawWaveform('reconstructedWave', data.time, data.reconstructed_signal, '#e74c3c', '重建模拟信号');

            // 显示采样值和二进制编码
            displaySampleValues(data.sampled_signal, data.quantized_signal, data.config.bit_depth);

            // 更新时间戳
            updateSectionTimestamp('adc_dac');
        })
        .catch(error => {
            console.error('Error updating waveforms:', error);
        });
    }

    function drawWaveform(canvasId, time, signal, color, label) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        const width = canvas.width;
        const height = canvas.height;

        // 清除画布
        ctx.clearRect(0, 0, width, height);

        // 绘制坐标轴
        ctx.beginPath();
        ctx.strokeStyle = '#ddd';
        ctx.lineWidth = 1;
        ctx.moveTo(30, height/2);
        ctx.lineTo(width-30, height/2);
        ctx.moveTo(30, 20);
        ctx.lineTo(30, height-20);
        ctx.stroke();

        // 绘制波形
        ctx.beginPath();
        ctx.strokeStyle = color;
        ctx.lineWidth = 2;

        for (let i = 0; i < time.length; i++) {
            const x = 30 + (time[i] * (width-60));
            const y = height/2 - (signal[i] * (height-40)/2);

            if (i === 0) {
                ctx.moveTo(x, y);
            } else {
                ctx.lineTo(x, y);
            }
        }

        ctx.stroke();

        // 添加标签
        ctx.fillStyle = '#333';
        ctx.font = '12px Arial';
        ctx.textAlign = 'left';
        ctx.fillText(label, 35, 20);
    }

    function drawSampledWaveform(canvasId, time, analogSignal, samplePoints, sampledSignal, quantizedSignal) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        const width = canvas.width;
        const height = canvas.height;

        // 清除画布
        ctx.clearRect(0, 0, width, height);

        // 绘制坐标轴
        ctx.beginPath();
        ctx.strokeStyle = '#ddd';
        ctx.lineWidth = 1;
        ctx.moveTo(30, height/2);
        ctx.lineTo(width-30, height/2);
        ctx.moveTo(30, 20);
        ctx.lineTo(30, height-20);
        ctx.stroke();

        // 绘制原始模拟信号（灰色虚线）
        ctx.beginPath();
        ctx.strokeStyle = '#ddd';
        ctx.lineWidth = 1;
        ctx.setLineDash([3, 2]);

        for (let i = 0; i < time.length; i++) {
            const x = 30 + (time[i] * (width-60));
            const y = height/2 - (analogSignal[i] * (height-40)/2);

            if (i === 0) {
                ctx.moveTo(x, y);
            } else {
                ctx.lineTo(x, y);
            }
        }

        ctx.stroke();
        ctx.setLineDash([]);

        // 绘制采样点
        ctx.fillStyle = '#e74c3c';
        for (let i = 0; i < samplePoints.length; i++) {
            const x = 30 + (samplePoints[i] * (width-60));
            const y = height/2 - (sampledSignal[i] * (height-40)/2);

            ctx.beginPath();
            ctx.arc(x, y, 4, 0, Math.PI * 2);
            ctx.fill();
        }

        // 绘制量化后的信号（阶梯状）
        ctx.beginPath();
        ctx.strokeStyle = '#2ecc71';
        ctx.lineWidth = 2;

        for (let i = 0; i < samplePoints.length; i++) {
            const x = 30 + (samplePoints[i] * (width-60));
            const y = height/2 - (quantizedSignal[i] * (height-40)/2);

            if (i === 0) {
                ctx.moveTo(x, y);
            } else {
                const prevX = 30 + (samplePoints[i-1] * (width-60));
                ctx.lineTo(prevX, y);
                ctx.lineTo(x, y);
            }
        }

        ctx.stroke();
    }

    function displaySampleValues(sampledSignal, quantizedSignal, bitDepth) {
        const sampleValuesDiv = document.getElementById('sample-values');
        const binaryValuesDiv = document.getElementById('binary-values');

        sampleValuesDiv.innerHTML = '';
        binaryValuesDiv.innerHTML = '';

        sampledSignal.forEach((value, index) => {
            const p = document.createElement('p');
            p.textContent = `采样点 ${index+1}: ${value.toFixed(4)} → ${quantizedSignal[index].toFixed(4)}`;
            sampleValuesDiv.appendChild(p);
        });

        quantizedSignal.forEach((value, index) => {
            const scaledValue = Math.round(value * (2**(bitDepth-1)-1));
            const binaryStr = (scaledValue >>> 0).toString(2).padStart(bitDepth, '0');

            const p = document.createElement('p');
            p.textContent = `采样点 ${index+1}: ${binaryStr}`;
            binaryValuesDiv.appendChild(p);
        });
    }

    // ==================== 子课题二：Turbo编码解码 ====================
    function initTurboCodec() {
        const encodeBtn = document.getElementById('encode-btn');
        const decodeBtn = document.getElementById('decode-btn');
        const compareBtn = document.getElementById('compare-btn');

        if (!encodeBtn || !decodeBtn || !compareBtn) return;

        encodeBtn.addEventListener('click', encodeText);
        decodeBtn.addEventListener('click', decodeText);
        compareBtn.addEventListener('click', compareResults);
    }

    function encodeText() {
        const text = document.getElementById('input-text').value;

        if (!text) {
            alert('请输入要编码的文本');
            return;
        }

        fetch('/encode_text', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({text: text})
        })
        .then(response => response.json())
        .then(data => {
            // 更新原始文本显示
            document.getElementById('original-text').textContent = data.original;

            // 更新编码数据
            const encodedDiv = document.getElementById('encoded-data');
            encodedDiv.innerHTML = formatBinaryData(data.encoded, 8);
            encodedDiv.dataset.encoded = data.encoded;

            // 更新性能指标
            document.getElementById('metric-efficiency').textContent = data.metrics.efficiency.toFixed(4);
            document.getElementById('metric-rate').textContent = data.metrics.rate;
            document.getElementById('metric-original-size').textContent = data.metrics.original_size + ' bits';
            document.getElementById('metric-encoded-size').textContent = data.metrics.encoded_size + ' bits';

            // 更新时间戳
            updateSectionTimestamp('coding');
        })
        .catch(error => {
            console.error('Error encoding text:', error);
        });
    }

    function decodeText() {
        const encodedData = document.getElementById('encoded-data').dataset.encoded;

        if (!encodedData) {
            alert('请先编码数据');
            return;
        }

        fetch('/decode_text', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({encoded_data: encodedData})
        })
        .then(response => response.json())
        .then(data => {
            // 更新解码文本
            document.getElementById('decoded-text').textContent = data.decoded;

            // 更新解码二进制数据
            const decodedBinaryDiv = document.getElementById('decoded-binary');
            decodedBinaryDiv.innerHTML = formatBinaryData(data.binary, 8);
            decodedBinaryDiv.dataset.decoded = data.decoded;

            // 更新性能指标
            document.getElementById('metric-ber').textContent = data.metrics.ber.toFixed(6);
            document.getElementById('metric-errors').textContent = data.metrics.errors;
            document.getElementById('metric-total-bits').textContent = data.metrics.total_bits;

            // 更新时间戳
            updateSectionTimestamp('coding');
        })
        .catch(error => {
            console.error('Error decoding text:', error);
        });
    }

    function compareResults() {
        const originalText = document.getElementById('original-text').textContent;
        const decodedText = document.getElementById('decoded-binary').dataset.decoded;

        if (!originalText || !decodedText) {
            alert('请先编码并解码数据');
            return;
        }

        const comparisonDiv = document.getElementById('comparison-result');

        if (originalText === decodedText) {
            comparisonDiv.className = 'alert alert-success';
            comparisonDiv.innerHTML = `
                <p><i class="fas fa-check-circle"></i> 解码成功 - 原始文本和解码文本完全匹配</p>
                <p>原始长度: ${originalText.length} 字符</p>
                <p>解码长度: ${decodedText.length} 字符</p>
            `;
        } else {
            comparisonDiv.className = 'alert alert-error';

            let diffPositions = [];
            const minLength = Math.min(originalText.length, decodedText.length);

            for (let i = 0; i < minLength; i++) {
                if (originalText[i] !== decodedText[i]) {
                    diffPositions.push(i);
                }
            }

            comparisonDiv.innerHTML = `
                <p><i class="fas fa-exclamation-triangle"></i> 解码错误 - 发现 ${diffPositions.length} 处差异</p>
                <p>原始长度: ${originalText.length} 字符 | 解码长度: ${decodedText.length} 字符</p>
                ${diffPositions.length > 0 ? `
                <p>差异位置: ${diffPositions.slice(0, 10).join(', ')}${diffPositions.length > 10 ? '...' : ''}</p>
                ` : ''}
            `;
        }

        // 更新时间戳
        updateSectionTimestamp('coding');
    }

    // ==================== 子课题三：TCP三次握手协议模拟 ====================
    function initProtocol() {
        const simulateBtn = document.getElementById('simulate-handshake-btn');
        const setLossRateBtn = document.getElementById('set-loss-rate-btn');

        if (!simulateBtn || !setLossRateBtn) return;

        simulateBtn.addEventListener('click', simulateTCPHandshake);
        setLossRateBtn.addEventListener('click', setPacketLossRate);

        // 初始状态
        updateHandshakeDiagram('reset');
    }

    function simulateTCPHandshake() {
        const clientIp = document.getElementById('client-ip').value || '192.168.1.100';
        const serverIp = document.getElementById('server-ip').value || '10.0.0.1';

        // 重置图表
        updateHandshakeDiagram('reset');

        fetch('/simulate_handshake', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                client_ip: clientIp,
                server_ip: serverIp
            })
        })
        .then(response => response.json())
        .then(data => {
            // 更新握手结果
            updateHandshakeResult(data);

            // 更新连接状态
            updateConnectionStatus(data);

            // 更新握手历史
            updateHandshakeHistory(data);

            // 更新性能指标
            updateProtocolMetrics(data);

            // 更新时间戳
            updateSectionTimestamp('protocol');
        })
        .catch(error => {
            console.error('Error simulating TCP handshake:', error);
        });
    }

    function setPacketLossRate() {
        const rate = parseFloat(document.getElementById('packet-loss-rate').value);

        if (isNaN(rate) || rate < 0 || rate > 1) {
            alert('请输入0到1之间的有效丢包率');
            return;
        }

        fetch('/set_packet_loss', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ rate: rate })
        })
        .then(response => response.json())
        .then(data => {
            if (data.status === 'success') {
                document.getElementById('current-loss-rate').textContent =
                    (data.new_rate * 100).toFixed(1) + '%';
                updateSectionTimestamp('protocol');
            }
        })
        .catch(error => {
            console.error('Error setting packet loss rate:', error);
        });
    }

    function updateHandshakeDiagram(state) {
        const synLine = document.getElementById('syn-line');
        const synAckLine = document.getElementById('syn-ack-line');
        const ackLine = document.getElementById('ack-line');

        // 重置所有线条
        synLine.style.opacity = '0.3';
        synAckLine.style.opacity = '0.3';
        ackLine.style.opacity = '0.3';
        synLine.style.color = '';
        synAckLine.style.color = '';
        ackLine.style.color = '';

        if (state === 'reset') return;

        // 根据握手状态更新图表
        if (state === 'SYN sent') {
            synLine.style.opacity = '1';
        } else if (state === 'SYN-ACK sent') {
            synLine.style.opacity = '1';
            synAckLine.style.opacity = '1';
        } else if (state === 'ACK sent') {
            synLine.style.opacity = '1';
            synAckLine.style.opacity = '1';
            ackLine.style.opacity = '1';
        } else if (state === 'SYN lost') {
            synLine.style.opacity = '1';
            synLine.style.color = '#e74c3c';
        } else if (state === 'SYN-ACK lost') {
            synLine.style.opacity = '1';
            synAckLine.style.opacity = '1';
            synAckLine.style.color = '#e74c3c';
        } else if (state === 'ACK lost') {
            synLine.style.opacity = '1';
            synAckLine.style.opacity = '1';
            ackLine.style.opacity = '1';
            ackLine.style.color = '#e74c3c';
        }
    }

    function updateHandshakeResult(data) {
        const resultDiv = document.getElementById('handshake-result');

        if (data.result.status === 'success') {
            resultDiv.innerHTML = `
                <div class="alert alert-success">
                    <p><i class="fas fa-check-circle"></i> 三次握手成功完成</p>
                    <p>连接ID: ${data.result.connection_id}</p>
                    <p>客户端初始序列号: ${data.result.client_isn}</p>
                    <p>服务器初始序列号: ${data.result.server_isn}</p>
                </div>
            `;
            updateHandshakeDiagram('ACK sent');
        } else {
            resultDiv.innerHTML = `
                <div class="alert alert-error">
                    <p><i class="fas fa-exclamation-triangle"></i> 握手失败: ${data.result.reason}</p>
                </div>
            `;

            if (data.result.reason.includes('SYN')) {
                updateHandshakeDiagram('SYN lost');
            } else if (data.result.reason.includes('SYN-ACK')) {
                updateHandshakeDiagram('SYN-ACK lost');
            } else if (data.result.reason.includes('ACK')) {
                updateHandshakeDiagram('ACK lost');
            }
        }
    }

        function updateConnectionStatus(data) {
        const statusDiv = document.getElementById('connection-status');
        statusDiv.innerHTML = '';

        if (Object.keys(data.connections).length === 0) {
            statusDiv.innerHTML = '<p>无活跃连接</p>';
            return;
        }

        const table = document.createElement('table');
        table.className = 'data-table';

        // 表头
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');
        ['连接ID', '客户端序列号', '服务器序列号', '状态'].forEach(text => {
            const th = document.createElement('th');
            th.textContent = text;
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);

        // 表体
        const tbody = document.createElement('tbody');
        for (const [id, conn] of Object.entries(data.connections)) {
            const row = document.createElement('tr');

            const idCell = document.createElement('td');
            idCell.textContent = id;
            row.appendChild(idCell);

            const clientSeqCell = document.createElement('td');
            clientSeqCell.textContent = conn.client_seq;
            row.appendChild(clientSeqCell);

            const serverSeqCell = document.createElement('td');
            serverSeqCell.textContent = conn.server_seq;
            row.appendChild(serverSeqCell);

            const statusCell = document.createElement('td');
            statusCell.textContent = conn.status;
            row.appendChild(statusCell);

            tbody.appendChild(row);
        }
        table.appendChild(tbody);

        statusDiv.appendChild(table);
    }

    function updateHandshakeHistory(data) {
        const historyDiv = document.getElementById('handshake-history');
        historyDiv.innerHTML = '';

        if (data.history.length === 0) {
            historyDiv.innerHTML = '<p>无历史记录</p>';
            return;
        }

        const table = document.createElement('table');
        table.className = 'data-table';

        // 表头
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');
        ['时间', '数据包类型', '源IP', '目的IP', '序列号', '确认号', '状态'].forEach(text => {
            const th = document.createElement('th');
            th.textContent = text;
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);

        // 表体
        const tbody = document.createElement('tbody');
        data.history.slice(-5).reverse().forEach(entry => {
            const row = document.createElement('tr');

            const timeCell = document.createElement('td');
            timeCell.textContent = new Date(entry.time).toLocaleTimeString();
            row.appendChild(timeCell);

            if (entry.packet) {
                const typeCell = document.createElement('td');
                typeCell.textContent = entry.packet.type;
                row.appendChild(typeCell);

                const srcCell = document.createElement('td');
                srcCell.textContent = entry.packet.src;
                row.appendChild(srcCell);

                const dstCell = document.createElement('td');
                dstCell.textContent = entry.packet.dst;
                row.appendChild(dstCell);

                const seqCell = document.createElement('td');
                seqCell.textContent = entry.packet.seq;
                row.appendChild(seqCell);

                const ackCell = document.createElement('td');
                ackCell.textContent = entry.packet.ack || '-';
                row.appendChild(ackCell);

                const statusCell = document.createElement('td');
                statusCell.textContent = '已发送';
                row.appendChild(statusCell);
            } else {
                const typeCell = document.createElement('td');
                typeCell.textContent = entry.phase;
                row.appendChild(typeCell);

                const emptyCell = document.createElement('td');
                emptyCell.colSpan = 4;
                emptyCell.textContent = '-';
                row.appendChild(emptyCell);

                const statusCell = document.createElement('td');
                statusCell.textContent = '已丢失';
                statusCell.style.color = '#e74c3c';
                row.appendChild(statusCell);
            }

            tbody.appendChild(row);
        });
        table.appendChild(tbody);

        historyDiv.appendChild(table);
    }

    function updateProtocolMetrics(data) {
        const metrics = document.querySelector('.protocol-metrics');
        if (!metrics) return;

        metrics.innerHTML = `
            <div class="metric">
                <span class="metric-label">当前丢包率:</span>
                <span id="current-loss-rate" class="metric-value">${(data.packet_loss_rate * 100).toFixed(1)}%</span>
            </div>
            <div class="metric">
                <span class="metric-label">活跃连接数:</span>
                <span id="active-connections" class="metric-value">${Object.keys(data.connections).length}</span>
            </div>
            <div class="metric">
                <span class="metric-label">握手成功率:</span>
                <span id="handshake-success-rate" class="metric-value">${data.success_rate ? data.success_rate.toFixed(1) + '%' : '--'}</span>
            </div>
        `;
    }

    // ==================== 子课题四：调制解调子系统 ====================
    function initModulation() {
        const generateBtn = document.getElementById('generate-modulation-btn');
        const modulationTypeSelect = document.getElementById('modulation-type');
        const noiseLevelSlider = document.getElementById('noise-level');
        const noiseLevelValue = document.getElementById('noise-level-value');
        const carrierFreqInput = document.getElementById('carrier-freq');

        if (!generateBtn || !modulationTypeSelect || !noiseLevelSlider) return;

        // 初始化噪声水平显示
        noiseLevelValue.textContent = noiseLevelSlider.value;

        noiseLevelSlider.addEventListener('input', function() {
            noiseLevelValue.textContent = this.value;
        });

        generateBtn.addEventListener('click', function() {
            // 生成随机二进制数据或使用编码子系统输出
            const inputText = document.getElementById('input-text').value;
            let binaryData;

            if (inputText) {
                // 如果有输入文本，使用Turbo编码的输出
                binaryData = document.getElementById('encoded-data').dataset.encoded || '';
            } else {
                // 否则生成随机二进制数据
                binaryData = '';
                for (let i = 0; i < 16; i++) {
                    binaryData += Math.random() > 0.5 ? '1' : '0';
                }
            }

            if (!binaryData) {
                alert('请先输入文本并编码或生成随机数据');
                return;
            }

            performModulationDemodulation(
                binaryData,
                modulationTypeSelect.value,
                parseFloat(noiseLevelSlider.value),
                parseInt(carrierFreqInput.value)
            );
        });
    }

    function performModulationDemodulation(binaryData, modulationType, noiseLevel, carrierFreq) {
        fetch('/modulate_demodulate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                binary_data: binaryData,
                modulation_type: modulationType,
                noise_level: noiseLevel,
                carrier_freq: carrierFreq
            })
        })
        .then(response => response.json())
        .then(data => {
            // 绘制基带信号
            drawDigitalSignal('basebandWave', data.baseband.time, data.baseband.signal, '#3498db', '基带信号');

            // 绘制I/Q信号
            drawDigitalSignal('iWave', data.iq_signals.time, data.iq_signals.i_signal, '#e74c3c', 'I路信号');
            drawDigitalSignal('qWave', data.iq_signals.time, data.iq_signals.q_signal, '#2ecc71', 'Q路信号');

            // 绘制调制信号
            drawAnalogSignal('modulatedWave', data.modulated.time, data.modulated.signal, '#9b59b6', modulationType + '调制信号');

            // 绘制含噪声信号
            drawNoisySignal('noisyWave', data.noisy_signal.time, data.noisy_signal.signal, '#f39c12', '含噪声信号');

            // 绘制星座图
            if (data.symbols) {
                drawConstellationDiagram(data.symbols);
            }

            // 更新解调结果
            updateDemodulationResults(data);

            // 更新性能指标
            updateModulationMetrics(data);

            // 更新时间戳
            updateSectionTimestamp('modulation');
        })
        .catch(error => {
            console.error('Error performing modulation/demodulation:', error);
        });
    }

    function drawDigitalSignal(canvasId, time, signal, color, label) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        const width = canvas.width;
        const height = canvas.height;

        ctx.clearRect(0, 0, width, height);

        // 绘制坐标轴
        ctx.beginPath();
        ctx.strokeStyle = '#ddd';
        ctx.lineWidth = 1;
        ctx.moveTo(30, height/2);
        ctx.lineTo(width-30, height/2);
        ctx.moveTo(30, 20);
        ctx.lineTo(30, height-20);
        ctx.stroke();

        // 绘制数字信号
        ctx.beginPath();
        ctx.strokeStyle = color;
        ctx.lineWidth = 2;

        for (let i = 0; i < time.length; i++) {
            const x = 30 + (time[i] / time[time.length-1] * (width-60));
            const y = height/2 - (signal[i] * (height-40)/2);

            if (i === 0) {
                ctx.moveTo(x, y);
            } else {
                // 绘制垂直线
                ctx.lineTo(x, height/2 - (signal[i-1] * (height-40)/2));
                ctx.lineTo(x, y);
            }
            ctx.lineTo(x, y);
        }

        ctx.stroke();

        // 添加标签
        ctx.fillStyle = '#333';
        ctx.font = '12px Arial';
        ctx.textAlign = 'left';
        ctx.fillText(label, 35, 20);
    }

    function drawAnalogSignal(canvasId, time, signal, color, label) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        const width = canvas.width;
        const height = canvas.height;

        ctx.clearRect(0, 0, width, height);

        // 绘制坐标轴
        ctx.beginPath();
        ctx.strokeStyle = '#ddd';
        ctx.lineWidth = 1;
        ctx.moveTo(30, height/2);
        ctx.lineTo(width-30, height/2);
        ctx.moveTo(30, 20);
        ctx.lineTo(30, height-20);
        ctx.stroke();

        // 绘制模拟信号
        ctx.beginPath();
        ctx.strokeStyle = color;
        ctx.lineWidth = 2;

        for (let i = 0; i < time.length; i++) {
            const x = 30 + (time[i] / time[time.length-1] * (width-60));
            const y = height/2 - (signal[i] * (height-40)/2);

            if (i === 0) {
                ctx.moveTo(x, y);
            } else {
                ctx.lineTo(x, y);
            }
        }

        ctx.stroke();

        // 添加标签
        ctx.fillStyle = '#333';
        ctx.font = '12px Arial';
        ctx.textAlign = 'left';
        ctx.fillText(label, 35, 20);
    }

    function drawNoisySignal(canvasId, time, signal, color, label) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        const width = canvas.width;
        const height = canvas.height;

        ctx.clearRect(0, 0, width, height);

        // 绘制坐标轴
        ctx.beginPath();
        ctx.strokeStyle = '#ddd';
        ctx.lineWidth = 1;
        ctx.moveTo(30, height/2);
        ctx.lineTo(width-30, height/2);
        ctx.stroke();

        // 绘制噪声信号
        ctx.beginPath();
        ctx.strokeStyle = color;
        ctx.lineWidth = 1;

        for (let i = 0; i < time.length; i++) {
            const x = 30 + (time[i] / time[time.length-1] * (width-60));
            const y = height/2 - (signal[i] * (height-40)/2);

            if (i === 0) {
                ctx.moveTo(x, y);
            } else {
                ctx.lineTo(x, y);
            }
        }

        ctx.stroke();

        // 添加标签
        ctx.fillStyle = '#333';
        ctx.font = '12px Arial';
        ctx.textAlign = 'left';
        ctx.fillText(label, 35, 20);
    }

    function drawConstellationDiagram(symbols) {
        const canvas = document.getElementById('constellationCanvas');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        const width = canvas.width;
        const height = canvas.height;

        ctx.clearRect(0, 0, width, height);

        // 绘制坐标轴
        ctx.beginPath();
        ctx.strokeStyle = '#ddd';
        ctx.lineWidth = 1;
        ctx.moveTo(width/2, 0);
        ctx.lineTo(width/2, height);
        ctx.moveTo(0, height/2);
        ctx.lineTo(width, height/2);
        ctx.stroke();

        // 绘制星座点
        symbols.forEach(symbol => {
            const x = width/2 + symbol[0] * (width/2 - 20);
            const y = height/2 - symbol[1] * (height/2 - 20);

            ctx.beginPath();
            ctx.arc(x, y, 5, 0, Math.PI * 2);
            ctx.fillStyle = '#e74c3c';
            ctx.fill();
        });

        // 添加标签
        ctx.fillStyle = '#333';
        ctx.font = '14px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('QPSK星座图', width/2, 20);
    }

    function updateDemodulationResults(data) {
        const encodedDiv = document.getElementById('encoded-data-mod');
        const demodulatedDiv = document.getElementById('demodulated-data');
        const comparisonDiv = document.getElementById('modulation-comparison');

        // 显示原始数据
        encodedDiv.textContent = formatBinaryData(data.binary_data, 8);

        // 显示解调数据
        demodulatedDiv.textContent = formatBinaryData(data.demodulated, 8);

        // 比较结果
        if (data.binary_data === data.demodulated) {
            comparisonDiv.className = 'alert alert-success';
            comparisonDiv.innerHTML = '<p><i class="fas fa-check-circle"></i> 解调成功 - 原始数据和解调数据完全匹配</p>';
        } else {
            comparisonDiv.className = 'alert alert-error';
            comparisonDiv.innerHTML = '<p><i class="fas fa-exclamation-triangle"></i> 解调错误 - 原始数据和解调数据不匹配</p>';
        }
    }

    function updateModulationMetrics(data) {
        document.getElementById('metric-modulation-type').textContent = data.modulation.modulation_type;
        document.getElementById('metric-noise-level').textContent = data.modulation.noise_level;
        document.getElementById('metric-ber-mod').textContent = data.metrics.ber.toFixed(6);
        document.getElementById('metric-snr').textContent = data.metrics.snr.toFixed(2) + ' dB';
    }

    // ==================== 子课题五：交换子系统 ====================
    function initSwitching() {
        const switchToABtn = document.getElementById('switch-to-a');
        const switchToBBtn = document.getElementById('switch-to-b');

        if (!switchToABtn || !switchToBBtn) return;

        switchToABtn.addEventListener('click', function() {
            switchRoute('ground_station_a');
        });

        switchToBBtn.addEventListener('click', function() {
            switchRoute('ground_station_b');
        });

        // 初始加载
        updateSwitchingMetrics();
    }

    function switchRoute(destination) {
        fetch('/switch_route', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ destination: destination })
        })
        .then(response => response.json())
        .then(data => {
            // 更新路由信息
            document.getElementById('current-path').textContent = `卫星 → ${data.route}`;
            document.getElementById('delay').textContent = `${data.latency}ms`;
            document.getElementById('capacity').textContent = `${data.capacity}Mbps`;

            // 更新路由历史
            updateRouteHistory(data);

            // 更新性能指标
            updateSwitchingMetrics(data);

            // 更新时间戳
            updateSectionTimestamp('switching');
        })
        .catch(error => {
            console.error('Error switching route:', error);
        });
    }

    function updateRouteHistory(data) {
        const historyDiv = document.getElementById('route-history');
        historyDiv.innerHTML = '';

        if (data.route_history.length === 0) {
            historyDiv.innerHTML = '<p>无路由历史</p>';
            return;
        }

        const table = document.createElement('table');
        table.className = 'data-table';

        // 表头
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');
        ['时间', '路由', '目标', '延迟', '容量'].forEach(text => {
            const th = document.createElement('th');
            th.textContent = text;
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);

        // 表体
        const tbody = document.createElement('tbody');
        data.route_history.slice(-5).reverse().forEach(entry => {
            const row = document.createElement('tr');

            const timeCell = document.createElement('td');
            timeCell.textContent = new Date(entry.time).toLocaleTimeString();
            row.appendChild(timeCell);

            const routeCell = document.createElement('td');
            routeCell.textContent = entry.route;
            row.appendChild(routeCell);

            const destCell = document.createElement('td');
            destCell.textContent = entry.destination;
            row.appendChild(destCell);

            const latencyCell = document.createElement('td');
            latencyCell.textContent = entry.latency + 'ms';
            row.appendChild(latencyCell);

            const capacityCell = document.createElement('td');
            capacityCell.textContent = entry.capacity + 'Mbps';
            row.appendChild(capacityCell);

            tbody.appendChild(row);
        });
        table.appendChild(tbody);

        historyDiv.appendChild(table);
    }

    function updateSwitchingMetrics(data) {
        const metrics = document.querySelector('.switching-metrics');
        if (!metrics) return;

        const trafficCount = data?.traffic?.length || 0;
        const balance = data?.metrics?.balance || 1;
        const avgLatency = data?.metrics?.avg_latency || 0;

        metrics.innerHTML = `
            <div class="metric">
                <span class="metric-label">流量计数:</span>
                <span id="switching-traffic-count" class="metric-value">${trafficCount}</span>
            </div>
            <div class="metric">
                <span class="metric-label">负载均衡:</span>
                <span id="switching-balance" class="metric-value">${(balance * 100).toFixed(1)}%</span>
            </div>
            <div class="metric">
                <span class="metric-label">平均延迟:</span>
                <span id="switching-avg-latency" class="metric-value">${avgLatency.toFixed(1)}ms</span>
            </div>
        `;
    }

    // ==================== 初始化所有模块 ====================
    initNavigation();
    initADDAConversion();
    initTurboCodec();
    initProtocol();
    initModulation();
    initSwitching();
    initAudioProcessing();
    checkSystemStatus();
    updateTimestamp();

    // 每秒更新时间戳
    setInterval(updateTimestamp, 1000);
});