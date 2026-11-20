import json
import http.client
import tempfile
import threading
import unittest
import urllib.request
import urllib.error
from pathlib import Path
from servidor_demo import crear_servidor

class ServicioTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.archivo = Path(self.tmp.name)/'datos.json'
        self.server = crear_servidor(self.archivo, 0)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.base = 'http://127.0.0.1:' + str(self.server.server_port)
        self.dto = dict(id='c35d3bbe-e818-4997-9457-a04e2e1ad5ed', titulo='Estudiar Room', descripcion='',
                        categoria='Estudios', prioridad='Alta', estado='Pendiente', fecha='2026-11-19T10:00:00', revision=1)
    def tearDown(self):
        self.server.shutdown(); self.server.server_close(); self.thread.join(); self.tmp.cleanup()
    def llamada(self, ruta, metodo='GET', datos=None):
        req = urllib.request.Request(self.base+ruta, data=json.dumps(datos).encode() if datos is not None else None,
                                     method=metodo, headers={'Content-Type':'application/json'})
        with urllib.request.urlopen(req, timeout=3) as r:
            body=r.read(); return r.status,json.loads(body) if body else None
    def test_put_repetido_no_duplica_y_persiste(self):
        conexion = http.client.HTTPConnection('127.0.0.1', self.server.server_port, timeout=3)
        for _ in range(2):
            conexion.request('PUT', '/recordatorios/'+self.dto['id'], json.dumps(self.dto), {'Content-Type':'application/json'})
            respuesta = conexion.getresponse()
            self.assertEqual(200,respuesta.status)
            self.assertEqual('close',respuesta.getheader('Connection'))
            respuesta.read()
        conexion.close()
        self.assertEqual(1,len(self.llamada('/recordatorios')[1]))
        self.assertEqual(1,len(json.loads(self.archivo.read_text())))
    def test_actualiza_sin_crear_otro_y_rechaza_version_antigua(self):
        self.llamada('/recordatorios/'+self.dto['id'], 'PUT', self.dto)
        self.llamada('/recordatorios/'+self.dto['id'], 'PUT', dict(self.dto,titulo='Actualizar Room',revision=2))
        with self.assertRaises(urllib.error.HTTPError) as error:
            self.llamada('/recordatorios/'+self.dto['id'], 'PUT', self.dto)
        self.assertEqual(409,error.exception.code)
        self.assertEqual(1,len(self.llamada('/recordatorios')[1]))
    def test_delete_es_idempotente(self):
        self.llamada('/recordatorios/'+self.dto['id'], 'PUT', self.dto)
        for _ in range(2): self.assertEqual(204,self.llamada('/recordatorios/'+self.dto['id'],'DELETE')[0])
        self.assertEqual([],self.llamada('/recordatorios')[1])
    def test_errores_http_controlados(self):
        for modo in ['400','500']:
            self.llamada('/control?modo='+modo)
            with self.assertRaises(urllib.error.HTTPError) as error: self.llamada('/recordatorios')
            self.assertEqual(int(modo),error.exception.code)

if __name__ == '__main__': unittest.main()
