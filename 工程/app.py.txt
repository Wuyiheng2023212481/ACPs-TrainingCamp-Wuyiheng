from flask import Flask, render_template, jsonify, request
import numpy as np
from datetime import datetime
import json
import random
import math

app = Flask(__name__)

# 存储当前配置
current_config = {
    'signal_type': 'sine',
    'frequency': 1,
    'amplitude': 1,
    'sample_rate': 8,
    'bit_depth': 8
}


# 子课题五：交换子系统
class SatelliteSwitch:
    def __init__(self):
        self.routes = {
            'ground_station_a': {'latency': 120, 'capacity': 10},
            'ground_station_b': {'latency': 150, 'capacity': 8},
            'relay': {'latency': 200, 'capacity': 5}
        }
        self.current_route = None
        self.traffic = []
        self.route_history = []

    def select_route(self, destination):
        """选择最佳路由"""
        if destination == 'ground_station_a':
            self.current_route = 'ground_station_a'
        elif destination == 'ground_station_b':
            if self.routes['ground_station_b']['latency'] < 180:
                self.current_route = 'ground_station_b'
            else:
                self.current_route = 'relay'

        # 记录路由历史
        self.route_history.append({
            'time': datetime.now().isoformat(),
            'route': self.current_route,
            'destination': destination
        })

        return self.routes[self.current_route]


switch = SatelliteSwitch()


# 子课题六：波束复用控制
class BeamController:
    def __init__(self):
        self.beam_angle = 45  # 初始角度
        self.beam_width = 30  # 波束宽度
        self.multiplexing = 'FDMA'  # 复用方式
        self.channels = 8  # 信道数
        self.beam_history = []
        self.multiplexing_history = []

    def adjust_beam(self, angle):
        """调整波束方向"""
        self.beam_angle = angle
        self.beam_history.append({
            'time': datetime.now().isoformat(),
            'angle': angle,
            'width': self.beam_width
        })
        return self.beam_angle

    def switch_multiplexing(self, mode):
        """切换复用方式"""
        self.multiplexing = mode
        self.multiplexing_history.append({
            'time': datetime.now().isoformat(),
            'mode': mode,
            'channels': self.channels
        })
        return self.multiplexing


beam_controller = BeamController()


# 子课题二：Turbo编码解码
class TurboCodec:
    def __init__(self):
        self.interleaver = lambda x: x[::-1]  # 简单逆序交织器
        self.constraint_length = 3
        self.generators = [0b101, 0b111]  # 生成多项式
        self.encoding_history = []
        self.decoding_history = []

    def conv_encode(self, bits, generator):
        """卷积编码器"""
        state = 0
        encoded = []
        for bit in bits:
            # 计算输出位
            output = 0
            mask = generator
            for _ in range(self.constraint_length):
                if mask & 1:
                    output ^= (state >> (self.constraint_length - 1)) & 1
                mask >>= 1
            output ^= bit
            encoded.append(output)

            # 更新状态
            state = ((state << 1) | bit) & ((1 << (self.constraint_length - 1)) - 1)
        return encoded

    def turbo_encode(self, text):
        """Turbo编码"""
        # 将文本转换为二进制
        binary_data = ''.join(format(ord(c), '08b') for c in text)
        bits = [int(b) for b in binary_data]

        # 第一个编码器
        encoded1 = self.conv_encode(bits, self.generators[0])

        # 交织后的第二个编码器
        interleaved = self.interleaver(bits.copy())
        encoded2 = self.conv_encode(interleaved, self.generators[1])

        # 记录编码历史
        self.encoding_history.append({
            'time': datetime.now().isoformat(),
            'text': text,
            'binary': binary_data,
            'encoded': ''.join(map(str, bits + encoded1 + encoded2))
        })

        return {
            'systematic': bits,
            'parity1': encoded1,
            'parity2': encoded2,
            'encoded': bits + encoded1 + encoded2
        }

    def turbo_decode(self, encoded_data, iterations=5):
        """Turbo解码"""
        # 分离系统位和校验位
        n = len(encoded_data) // 3
        systematic = encoded_data[:n]
        parity1 = encoded_data[n:2 * n]
        parity2 = encoded_data[2 * n:]

        # 初始软判决
        llr = [0.0] * n

        # 迭代解码
        for _ in range(iterations):
            # 第一个解码器
            for i in range(n):
                llr[i] += systematic[i] * 2 - 1  # 系统位贡献
                llr[i] += parity1[i] * 2 - 1  # 第一个校验位贡献

            # 交织
            interleaved_llr = self.interleaver(llr.copy())

            # 第二个解码器
            for i in range(n):
                interleaved_llr[i] += parity2[i] * 2 - 1  # 第二个校验位贡献

            # 解交织
            llr = self.interleaver(interleaved_llr.copy())

        # 硬判决
        decoded = [1 if l > 0 else 0 for l in llr]

        # 记录解码历史
        self.decoding_history.append({
            'time': datetime.now().isoformat(),
            'encoded': ''.join(map(str, encoded_data)),
            'decoded': ''.join(map(str, decoded))
        })

        return decoded

    def binary_to_text(self, binary):
        """二进制转文本"""
        chars = []
        for i in range(0, len(binary), 8):
            byte = binary[i:i + 8]
            if len(byte) == 8:
                chars.append(chr(int(''.join(map(str, byte)), 2)))
        return ''.join(chars)


turbo_codec = TurboCodec()


# 子课题三：TCP三次握手协议模拟
class TCPProtocol:
    def __init__(self):
        self.connections = {}
        self.packet_loss_rate = 0.1  # 默认丢包率10%
        self.handshake_history = []
        self.success_count = 0
        self.total_attempts = 0

    def simulate_handshake(self, client_ip, server_ip):
        """模拟TCP三次握手过程"""
        self.total_attempts += 1

        # 生成随机序列号
        client_isn = random.randint(0, 2 ** 32 - 1)
        server_isn = random.randint(0, 2 ** 32 - 1)

        # 第一次握手：SYN
        if random.random() > self.packet_loss_rate:
            syn_packet = {
                'type': 'SYN',
                'seq': client_isn,
                'ack': 0,
                'src': client_ip,
                'dst': server_ip
            }
            self.handshake_history.append({
                'time': datetime.now().isoformat(),
                'packet': syn_packet,
                'status': 'sent'
            })
        else:
            self.handshake_history.append({
                'time': datetime.now().isoformat(),
                'packet': None,
                'status': 'lost',
                'phase': 'SYN'
            })
            return {'status': 'failed', 'reason': 'SYN packet lost'}

        # 第二次握手：SYN-ACK
        if random.random() > self.packet_loss_rate:
            syn_ack_packet = {
                'type': 'SYN-ACK',
                'seq': server_isn,
                'ack': client_isn + 1,
                'src': server_ip,
                'dst': client_ip
            }
            self.handshake_history.append({
                'time': datetime.now().isoformat(),
                'packet': syn_ack_packet,
                'status': 'sent'
            })
        else:
            self.handshake_history.append({
                'time': datetime.now().isoformat(),
                'packet': None,
                'status': 'lost',
                'phase': 'SYN-ACK'
            })
            return {'status': 'failed', 'reason': 'SYN-ACK packet lost'}

        # 第三次握手：ACK
        if random.random() > self.packet_loss_rate:
            ack_packet = {
                'type': 'ACK',
                'seq': client_isn + 1,
                'ack': server_isn + 1,
                'src': client_ip,
                'dst': server_ip
            }
            self.handshake_history.append({
                'time': datetime.now().isoformat(),
                'packet': ack_packet,
                'status': 'sent'
            })

            # 连接建立成功
            connection_id = f"{client_ip}:{server_ip}"
            self.connections[connection_id] = {
                'client_seq': client_isn + 1,
                'server_seq': server_isn + 1,
                'status': 'established',
                'last_activity': datetime.now().isoformat()
            }

            self.success_count += 1
            return {
                'status': 'success',
                'connection_id': connection_id,
                'client_isn': client_isn,
                'server_isn': server_isn
            }
        else:
            self.handshake_history.append({
                'time': datetime.now().isoformat(),
                'packet': None,
                'status': 'lost',
                'phase': 'ACK'
            })
            return {'status': 'failed', 'reason': 'ACK packet lost'}

    def set_packet_loss_rate(self, rate):
        """设置丢包率"""
        self.packet_loss_rate = rate
        return {'status': 'success', 'new_rate': rate}

    def get_success_rate(self):
        """计算握手成功率"""
        if self.total_attempts == 0:
            return 0
        return (self.success_count / self.total_attempts) * 100


tcp_protocol = TCPProtocol()


# 子课题四：调制解调子系统
class Modem:
    def __init__(self):
        self.modulation_history = []
        self.demodulation_history = []
        self.noise_level = 0.1  # 默认噪声水平
        self.modulation_type = 'QPSK'  # 默认调制方式
        self.carrier_freq = 10  # 载波频率
        self.samples_per_symbol = 100  # 每个符号的采样点数
        self.symbol_rate = 1  # 符号速率(Hz)

    def add_noise(self, signal, noise_level=None):
        """添加高斯白噪声"""
        noise_level = noise_level or self.noise_level
        noise = np.random.normal(0, noise_level, len(signal))
        return signal + noise

    def generate_baseband(self, bits):
        """生成基带信号"""
        time = np.linspace(0, len(bits), len(bits) * self.samples_per_symbol)
        baseband = np.zeros_like(time)

        for i, bit in enumerate(bits):
            start = i * self.samples_per_symbol
            end = (i + 1) * self.samples_per_symbol
            baseband[start:end] = bit * 2 - 1  # 0 -> -1, 1 -> 1

        return time, baseband

    def generate_iq_signals(self, bits):
        """生成I/Q路信号"""
        # 确保比特数为偶数
        if len(bits) % 2 != 0:
            bits = np.append(bits, 0)

        time = np.linspace(0, len(bits) // 2, len(bits) // 2 * self.samples_per_symbol)
        i_signal = np.zeros_like(time)
        q_signal = np.zeros_like(time)

        for i in range(0, len(bits), 2):
            dibit = bits[i:i + 2]
            symbol_idx = i // 2
            start = symbol_idx * self.samples_per_symbol
            end = (symbol_idx + 1) * self.samples_per_symbol

            # QPSK映射: 00 -> (1,1), 01 -> (-1,1), 10 -> (1,-1), 11 -> (-1,-1)
            i_bit = dibit[0] if len(dibit) > 0 else 0
            q_bit = dibit[1] if len(dibit) > 1 else 0

            i_signal[start:end] = i_bit * 2 - 1
            q_signal[start:end] = q_bit * 2 - 1

        return time, i_signal, q_signal

    def qpsk_modulate(self, bits):
        """QPSK调制"""
        # 确保比特数为偶数
        if len(bits) % 2 != 0:
            bits = np.append(bits, 0)

        # 生成I/Q信号
        time, i_signal, q_signal = self.generate_iq_signals(bits)

        # 生成载波
        carrier_i = np.cos(2 * np.pi * self.carrier_freq * time)
        carrier_q = np.sin(2 * np.pi * self.carrier_freq * time)

        # 调制信号
        modulated = i_signal * carrier_i + q_signal * carrier_q

        # 生成星座点
        symbols = []
        for i in range(0, len(bits), 2):
            dibit = bits[i:i + 2]
            if len(dibit) == 2:
                i_val = dibit[0] * 2 - 1  # 0->-1, 1->1
                q_val = dibit[1] * 2 - 1
                symbols.append((i_val, q_val))

        return time, modulated, symbols, i_signal, q_signal

    def demodulate(self, signal, modulation_type=None):
        """解调信号"""
        modulation_type = modulation_type or self.modulation_type
        if modulation_type == 'BPSK':
            return self.bpsk_demodulate(signal)
        else:
            return self.qpsk_demodulate(signal)

    def qpsk_demodulate(self, signal):
        """QPSK解调"""
        bits = []
        symbol_length = self.samples_per_symbol
        num_symbols = len(signal) // symbol_length

        for i in range(num_symbols):
            start = i * symbol_length
            end = (i + 1) * symbol_length
            symbol = signal[start:end]

            # 相干解调 - I路
            t = np.linspace(0, 1, len(symbol))
            i_carrier = np.cos(2 * np.pi * self.carrier_freq * t)
            i_product = symbol * i_carrier
            i_integral = np.sum(i_product)

            # 相干解调 - Q路
            q_carrier = np.sin(2 * np.pi * self.carrier_freq * t)
            q_product = symbol * q_carrier
            q_integral = np.sum(q_product)

            # 判决
            bit1 = 1 if i_integral > 0 else 0
            bit2 = 1 if q_integral > 0 else 0
            bits.extend([bit1, bit2])

        return bits


modem = Modem()


@app.route('/')
def index():
    return render_template('index.html')


# 子课题一：AD/DA转换
@app.route('/get_waveform', methods=['POST'])
def get_waveform():
    config = request.json
    current_config.update(config)

    # 生成模拟信号
    time = np.linspace(0, 1, 1000)
    if config['signal_type'] == 'sine':
        analog_signal = config['amplitude'] * np.sin(2 * np.pi * config['frequency'] * time)
    elif config['signal_type'] == 'square':
        analog_signal = config['amplitude'] * np.sign(np.sin(2 * np.pi * config['frequency'] * time))
    elif config['signal_type'] == 'sawtooth':
        analog_signal = config['amplitude'] * (
                2 * (time * config['frequency'] - np.floor(0.5 + time * config['frequency'])))
    elif config['signal_type'] == 'triangle':
        analog_signal = config['amplitude'] * (
                2 * np.abs(2 * (time * config['frequency'] - np.floor(time * config['frequency'] + 0.5))) - 1)
    elif config['signal_type'] == 'noise':
        analog_signal = config['amplitude'] * np.random.normal(0, 0.5, len(time))

    # 采样
    sample_points = np.linspace(0, 1, int(config['sample_rate'] * 10))  # 10个周期
    if config['signal_type'] == 'sine':
        sampled_signal = config['amplitude'] * np.sin(2 * np.pi * config['frequency'] * sample_points)
    elif config['signal_type'] == 'square':
        sampled_signal = config['amplitude'] * np.sign(np.sin(2 * np.pi * config['frequency'] * sample_points))
    elif config['signal_type'] == 'sawtooth':
        sampled_signal = config['amplitude'] * (
                2 * (sample_points * config['frequency'] - np.floor(0.5 + sample_points * config['frequency'])))
    elif config['signal_type'] == 'triangle':
        sampled_signal = config['amplitude'] * (2 * np.abs(
            2 * (sample_points * config['frequency'] - np.floor(sample_points * config['frequency'] + 0.5))) - 1)
    elif config['signal_type'] == 'noise':
        sampled_signal = config['amplitude'] * np.random.normal(0, 0.5, len(sample_points))

    # 量化
    quantized_signal = np.round(sampled_signal * (2 ** (config['bit_depth'] - 1) - 1)) / (
            2 ** (config['bit_depth'] - 1) - 1)

    # 重建信号
    reconstructed_signal = np.interp(time, sample_points, quantized_signal)

    # 计算SNR
    if len(reconstructed_signal) == len(analog_signal):
        mse = np.mean((analog_signal - reconstructed_signal) ** 2)
        signal_power = np.mean(analog_signal ** 2)
        snr = 10 * np.log10(signal_power / mse) if mse > 0 else float('inf')
    else:
        snr = 0

    return jsonify({
        'time': time.tolist(),
        'analog_signal': analog_signal.tolist(),
        'sample_points': sample_points.tolist(),
        'sampled_signal': sampled_signal.tolist(),
        'quantized_signal': quantized_signal.tolist(),
        'reconstructed_signal': reconstructed_signal.tolist(),
        'snr': snr,
        'config': current_config
    })


# 子课题二：Turbo编码解码
@app.route('/encode_text', methods=['POST'])
def encode_text():
    text = request.json.get('text', '')

    # 执行Turbo编码
    encoded = turbo_codec.turbo_encode(text)

    # 计算编码效率
    original_size = len(text) * 8
    encoded_size = len(encoded['encoded'])
    efficiency = original_size / encoded_size

    return jsonify({
        'original': text,
        'binary': ''.join(map(str, encoded['systematic'])),
        'encoded': ''.join(map(str, encoded['encoded'])),
        'details': {
            'systematic': ''.join(map(str, encoded['systematic'])),
            'parity1': ''.join(map(str, encoded['parity1'])),
            'parity2': ''.join(map(str, encoded['parity2']))
        },
        'metrics': {
            'efficiency': efficiency,
            'original_size': original_size,
            'encoded_size': encoded_size,
            'rate': '1/3'
        },
        'timestamp': datetime.now().isoformat()
    })


@app.route('/decode_text', methods=['POST'])
def decode_text():
    encoded_data = request.json.get('encoded_data', '')
    binary_data = [int(bit) for bit in encoded_data]

    # 执行Turbo解码
    decoded = turbo_codec.turbo_decode(binary_data)

    # 计算误码率
    n = len(binary_data) // 3
    systematic = binary_data[:n]
    errors = sum(1 for a, b in zip(systematic, decoded[:n]) if a != b)
    ber = errors / n if n > 0 else 0

    # 转换为文本
    decoded_text = turbo_codec.binary_to_text(decoded)

    return jsonify({
        'decoded': decoded_text,
        'binary': ''.join(map(str, decoded)),
        'metrics': {
            'ber': ber,
            'errors': errors,
            'total_bits': n
        },
        'timestamp': datetime.now().isoformat()
    })


# 子课题三：TCP三次握手协议模拟
@app.route('/simulate_handshake', methods=['POST'])
def simulate_handshake():
    data = request.json
    client_ip = data.get('client_ip', '192.168.1.100')
    server_ip = data.get('server_ip', '10.0.0.1')

    result = tcp_protocol.simulate_handshake(client_ip, server_ip)
    return jsonify({
        'result': result,
        'history': tcp_protocol.handshake_history[-3:],
        'connections': tcp_protocol.connections,
        'packet_loss_rate': tcp_protocol.packet_loss_rate,
        'success_rate': tcp_protocol.get_success_rate(),
        'timestamp': datetime.now().isoformat()
    })


@app.route('/set_packet_loss', methods=['POST'])
def set_packet_loss():
    rate = request.json.get('rate', 0.1)
    if 0 <= rate <= 1:
        result = tcp_protocol.set_packet_loss_rate(rate)
        return jsonify({
            'status': 'success',
            'new_rate': rate,
            'success_rate': tcp_protocol.get_success_rate(),
            'timestamp': datetime.now().isoformat()
        })
    else:
        return jsonify({
            'status': 'error',
            'message': 'Invalid rate, must be between 0 and 1',
            'timestamp': datetime.now().isoformat()
        })


# 子课题四：调制解调子系统
@app.route('/modulate_demodulate', methods=['POST'])
def modulate_demodulate():
    data = request.json
    binary_data = data.get('binary_data', '')
    modulation_type = data.get('modulation_type', 'QPSK')
    noise_level = data.get('noise_level', 0.1)

    # 转换二进制字符串为列表
    bits = [int(bit) for bit in binary_data]

    # 设置调制解调器参数
    modem.modulation_type = modulation_type
    modem.noise_level = noise_level

    # 生成基带信号
    baseband_time, baseband_signal = modem.generate_baseband(bits)

    # 生成I/Q信号
    iq_time, i_signal, q_signal = modem.generate_iq_signals(bits)

    # 调制
    if modulation_type == 'BPSK':
        time, modulated = modem.bpsk_modulate(bits)
        symbols = None
    else:
        time, modulated, symbols, i_signal, q_signal = modem.qpsk_modulate(bits)

    # 添加噪声
    noisy_signal = modem.add_noise(modulated, noise_level)

    # 解调
    demodulated_bits = modem.demodulate(noisy_signal, modulation_type)

    # 计算误码率 (BER)
    original_bits = bits[:len(demodulated_bits)]  # 确保长度一致
    errors = sum(1 for a, b in zip(original_bits, demodulated_bits) if a != b)
    ber = errors / len(original_bits) if original_bits else 0

    # 计算信噪比 (SNR)
    signal_power = np.mean(modulated ** 2)
    noise_power = np.mean((noisy_signal - modulated) ** 2)
    snr = 10 * np.log10(signal_power / noise_power) if noise_power > 0 else float('inf')

    # 保存历史记录
    modem.modulation_history.append({
        'time': datetime.now().isoformat(),
        'input': binary_data,
        'modulation_type': modulation_type,
        'output': modulated.tolist()
    })

    modem.demodulation_history.append({
        'time': datetime.now().isoformat(),
        'input': noisy_signal.tolist(),
        'output': ''.join(map(str, demodulated_bits)),
        'ber': ber,
        'snr': snr
    })

    return jsonify({
        'baseband': {
            'time': baseband_time.tolist(),
            'signal': baseband_signal.tolist()
        },
        'iq_signals': {
            'time': iq_time.tolist(),
            'i_signal': i_signal.tolist(),
            'q_signal': q_signal.tolist()
        },
        'modulated': {
            'time': time.tolist(),
            'signal': modulated.tolist()
        },
        'noisy_signal': {
            'time': time.tolist(),
            'signal': noisy_signal.tolist()
        },
        'symbols': symbols,
        'demodulated': ''.join(map(str, demodulated_bits)),
        'metrics': {
            'ber': ber,
            'snr': snr,
            'errors': errors,
            'total_bits': len(original_bits)
        },
        'timestamp': datetime.now().isoformat()
    })

@app.route('/set_modem_config', methods=['POST'])
def set_modem_config():
    data = request.json
    modem.modulation_type = data.get('modulation_type', modem.modulation_type)
    modem.noise_level = data.get('noise_level', modem.noise_level)
    modem.carrier_freq = data.get('carrier_freq', modem.carrier_freq)
    modem.samples_per_symbol = data.get('samples_per_symbol', modem.samples_per_symbol)

    return jsonify({
        'status': 'success',
        'modulation_type': modem.modulation_type,
        'noise_level': modem.noise_level,
        'carrier_freq': modem.carrier_freq,
        'samples_per_symbol': modem.samples_per_symbol,
        'timestamp': datetime.now().isoformat()
    })


# 子课题五：交换子系统
@app.route('/switch_route', methods=['POST'])
def switch_route():
    destination = request.json.get('destination', 'ground_station_a')

    # 选择路由
    route = switch.select_route(destination)

    # 模拟流量
    traffic = {
        'time': datetime.now().isoformat(),
        'destination': destination,
        'size': np.random.randint(1, 5),
        'latency': route['latency']
    }
    switch.traffic.append(traffic)

    # 计算负载均衡指标
    traffic_count = len(switch.traffic)
    a_traffic = len([t for t in switch.traffic if t['destination'] == 'ground_station_a'])
    b_traffic = len([t for t in switch.traffic if t['destination'] == 'ground_station_b'])
    balance = 1 - abs(a_traffic - b_traffic) / traffic_count if traffic_count > 0 else 1

    return jsonify({
        'route': switch.current_route,
        'latency': route['latency'],
        'capacity': route['capacity'],
        'traffic': switch.traffic[-10:],  # 返回最近10条记录
        'route_history': switch.route_history[-5:],  # 返回最近5条路由历史
        'metrics': {
            'traffic_count': traffic_count,
            'balance': balance,
            'avg_latency': np.mean([t['latency'] for t in switch.traffic[-10:]]) if switch.traffic[-10:] else 0
        },
        'timestamp': datetime.now().isoformat()
    })

# 子课题六：波束复用控制
@app.route('/control_beam', methods=['POST'])
def control_beam():
    action = request.json.get('action')
    value = request.json.get('value')

    if action == 'adjust_angle':
        angle = beam_controller.adjust_beam(value)
        return jsonify({
            'beam_angle': angle,
            'beam_width': beam_controller.beam_width,
            'beam_history': beam_controller.beam_history[-5:],
            'metrics': {
                'coverage': calculate_coverage(angle, beam_controller.beam_width),
                'pointing_accuracy': 1.0  # 简化模型
            },
            'timestamp': datetime.now().isoformat()
        })
    elif action == 'switch_multiplexing':
        mode = beam_controller.switch_multiplexing(value)
        return jsonify({
            'multiplexing': mode,
            'channels': beam_controller.channels,
            'multiplexing_history': beam_controller.multiplexing_history[-5:],
            'metrics': {
                'capacity': calculate_capacity(mode, beam_controller.channels),
                'efficiency': calculate_efficiency(mode)
            },
            'timestamp': datetime.now().isoformat()
        })

def calculate_coverage(angle, width):
    """简化的覆盖范围计算"""
    return width / 360.0

def calculate_capacity(mode, channels):
    """简化的容量计算"""
    if mode == 'FDMA':
        return channels * 5  # 假设每个信道5Mbps
    else:  # TDMA
        return channels * 2  # 假设每个时隙2Mbps

def calculate_efficiency(mode):
    """简化的效率计算"""
    return 0.8 if mode == 'FDMA' else 0.9

# 获取系统状态
@app.route('/system_status', methods=['GET'])
def system_status():
    return jsonify({
        'adc_dac': current_config,
        'coding': {
            'last_encode': turbo_codec.encoding_history[-1] if turbo_codec.encoding_history else None,
            'last_decode': turbo_codec.decoding_history[-1] if turbo_codec.decoding_history else None
        },
        'protocol': {
            'active_connections': len(tcp_protocol.connections),
            'packet_loss_rate': tcp_protocol.packet_loss_rate,
            'success_rate': tcp_protocol.get_success_rate()
        },
        'modulation': {
            'last_modulation': modem.modulation_history[-1] if modem.modulation_history else None,
            'last_demodulation': modem.demodulation_history[-1] if modem.demodulation_history else None
        },
        'switching': {
            'current_route': switch.current_route,
            'traffic_count': len(switch.traffic)
        },
        'beamforming': {
            'current_angle': beam_controller.beam_angle,
            'current_mode': beam_controller.multiplexing
        },
        'timestamp': datetime.now().isoformat()
    })


# 在app.py中添加以下路由（放在其他路由附近）
@app.route('/process_audio_signal', methods=['GET'])
def process_audio_signal():
    # 生成随机音频信号
    time = np.linspace(0, 1, 1000)
    freq = random.uniform(1, 5)  # 随机频率1-5Hz
    analog_signal = np.sin(2 * np.pi * freq * time)

    # AD转换
    sample_rate = random.choice([8, 16, 32])
    sample_points = np.linspace(0, 1, sample_rate * 10)
    sampled_signal = np.sin(2 * np.pi * freq * sample_points)
    bit_depth = 8
    quantized_signal = np.round(sampled_signal * (2 ** (bit_depth - 1) - 1)) / (2 ** (bit_depth - 1) - 1)

    # Turbo编码
    text = "AudioSignal" + str(random.randint(100, 999))
    encoded = turbo_codec.turbo_encode(text)

    # TCP握手
    handshake_result = tcp_protocol.simulate_handshake(
        f"192.168.1.{random.randint(1, 255)}",
        f"10.0.0.{random.randint(1, 255)}"
    )

    # 调制
    bits = encoded['encoded']
    _, modulated, symbols, _, _ = modem.qpsk_modulate(bits)

    # 交换路由
    destination = random.choice(['ground_station_a', 'ground_station_b'])
    route = switch.select_route(destination)

    # 波束控制
    angle = random.randint(30, 60)
    beam_controller.adjust_beam(angle)

    return jsonify({
        'time': time.tolist(),
        'analog_signal': analog_signal.tolist(),
        'sample_points': sample_points.tolist(),
        'sampled_signal': sampled_signal.tolist(),
        'quantized_signal': quantized_signal.tolist(),
        'encoded': ''.join(map(str, bits)),
        'handshake_result': handshake_result,
        'modulated': modulated.tolist(),
        'route': route,
        'beam_angle': angle,
        'timestamp': datetime.now().isoformat()
    })




if __name__ == '__main__':
    app.run(debug=True)