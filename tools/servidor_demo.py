"""Servicio de práctica sin dependencias externas. No es un servidor de producción."""
import argparse
import json
import threading
import time
import uuid
import os
import shutil
import subprocess
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse, parse_qs


class Estado:
    def __init__(self, archivo):
        self.archivo = Path(archivo)
        self.lock = threading.RLock()
        self.registros = json.loads(self.archivo.read_text(encoding='utf-8')) if self.archivo.exists() else {}
        self.modo = 'normal'

    def persistir(self):
        self.archivo.parent.mkdir(parents=True, exist_ok=True)
        temporal = self.archivo.with_suffix('.tmp')
        temporal.write_text(json.dumps(self.registros, ensure_ascii=False, indent=2), encoding='utf-8')
        temporal.replace(self.archivo)


class Handler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'
    def responder(self, codigo, datos=None):
        contenido = json.dumps(datos, ensure_ascii=False).encode('utf-8') if datos is not None else b''
        try:
            self.send_response(codigo)
            self.send_header('Content-Type', 'application/json; charset=utf-8')
            self.send_header('Content-Length', str(len(contenido)))
            self.send_header('Connection', 'close')
            self.close_connection = True
            self.end_headers()
            self.wfile.write(contenido)
            self.wfile.flush()
        except (BrokenPipeError, ConnectionResetError):
            pass  # Un timeout del cliente no deshace una operación ya recibida.

    def fallo(self):
        modo = self.server.estado.modo
        if modo in ('400', '500'):
            self.responder(int(modo), {'mensaje': 'Error HTTP simulado'})
            return True
        if modo == 'timeout':
            time.sleep(6)
        return False

    def do_GET(self):
        ruta = urlparse(self.path)
        s = self.server.estado
        if ruta.path == '/control':
            modo = parse_qs(ruta.query).get('modo', ['normal'])[0]
            if modo not in ('normal', '400', '500', 'timeout'):
                return self.responder(400, {'mensaje': 'Modo inválido'})
            s.modo = modo
            return self.responder(200, {'modo': modo})
        if ruta.path == '/estado':
            with s.lock:
                return self.responder(200, {'modo': s.modo, 'cantidad': len(s.registros)})
        if ruta.path != '/recordatorios':
            return self.responder(404, {'mensaje': 'Ruta inexistente'})
        if self.fallo(): return
        with s.lock:
            return self.responder(200, list(s.registros.values()))

    def do_PUT(self):
        if self.fallo(): return
        try:
            identificador = self.path.removeprefix('/recordatorios/')
            uuid.UUID(identificador)
            datos = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
            assert datos['id'] == identificador
            assert len(datos['titulo'].strip()) >= 4 and len(datos['descripcion']) <= 200
            assert datos['categoria'] in ('Personal', 'Trabajo', 'Salud', 'Estudios')
            assert datos['prioridad'] in ('Baja', 'Media', 'Alta')
            assert datos['estado'] in ('Pendiente', 'En curso', 'Completado')
            assert isinstance(datos['revision'], int) and datos['revision'] > 0
        except (ValueError, KeyError, AssertionError, TypeError):
            return self.responder(400, {'mensaje': 'Registro inválido'})
        s = self.server.estado
        with s.lock:
            anterior = s.registros.get(identificador)
            if anterior and (datos['revision'] < anterior['revision'] or
                (datos['revision'] == anterior['revision'] and datos != anterior)):
                return self.responder(409, {'mensaje': 'Revisión desactualizada o en conflicto'})
            s.registros[identificador] = datos
            s.persistir()
            return self.responder(200, datos)

    def do_DELETE(self):
        if self.fallo(): return
        try:
            identificador = self.path.removeprefix('/recordatorios/')
            uuid.UUID(identificador)
        except ValueError:
            return self.responder(400, {'mensaje': 'Identificador inválido'})
        s = self.server.estado
        with s.lock:
            s.registros.pop(identificador, None)
            s.persistir()
        self.responder(204)


def crear_servidor(archivo, puerto=8765, host='127.0.0.1'):
    server = ThreadingHTTPServer((host, puerto), Handler)
    server.estado = Estado(archivo)
    return server


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--puerto', type=int, default=8765)
    parser.add_argument('--host', default='127.0.0.1')
    parser.add_argument('--datos', default=str(Path(__file__).with_name('datos_servidor.json')))
    args = parser.parse_args()
    server = crear_servidor(args.datos, args.puerto, args.host)
    # Túnel del emulador o teléfono USB hacia la PC, sin abrir puertos de red.
    sdk = Path(os.environ.get('ANDROID_HOME', str(Path(os.environ.get('LOCALAPPDATA', ''))/'Android'/'Sdk')))
    adb = shutil.which('adb') or str(sdk/'platform-tools'/'adb.exe')
    if Path(adb).exists():
        try:
            resultado = subprocess.run([adb, 'reverse', f'tcp:{args.puerto}', f'tcp:{args.puerto}'], timeout=10, check=False)
            if resultado.returncode: print('Abrir el emulador y ejecutar adb reverse tcp:8765 tcp:8765 antes de sincronizar.', flush=True)
        except (OSError, subprocess.TimeoutExpired):
            print('No se pudo preparar ADB. Consultar las instrucciones del README.', flush=True)
    print(f'Servidor de práctica iniciado en http://{args.host}:{args.puerto}', flush=True)
    try: server.serve_forever()
    except KeyboardInterrupt: pass
    finally: server.server_close()
