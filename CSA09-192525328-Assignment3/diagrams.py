from PIL import Image, ImageDraw, ImageFont
import math
F = lambda s, b=False: ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans%s.ttf' % ('-Bold' if b else ''), s)
M = lambda s: ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf', s)

class Canvas:
    def __init__(self, w, h, title):
        self.im = Image.new('RGB', (w, h), 'white'); self.d = ImageDraw.Draw(self.im)
        self.d.text((20, 12), title, font=F(20, True), fill=(11, 61, 145))
    def box(self, x, y, w, h, title, lines=(), fill=(232, 240, 252), outline=(60, 60, 60), tfont=None, bold=True):
        d = self.d
        d.rectangle([x, y, x + w, y + h], fill=fill, outline=outline, width=2)
        tf = tfont or F(14, bold)
        tw = d.textlength(title, font=tf)
        d.text((x + (w - tw) / 2, y + 6), title, font=tf, fill='black')
        if lines:
            ty = y + 6 + tf.size + 8
            d.line([x, ty - 3, x + w, ty - 3], fill=outline, width=1)
            for l in lines:
                d.text((x + 8, ty), l, font=F(12), fill='black'); ty += 16
        return (x, y, w, h)
    def center(self, b): return (b[0] + b[2] / 2, b[1] + b[3] / 2)
    def edge(self, b, toward):
        x, y, w, h = b; cx, cy = x + w / 2, y + h / 2; dx, dy = toward[0] - cx, toward[1] - cy
        if dx == 0 and dy == 0: return cx, cy
        sx = (w / 2) / abs(dx) if dx else 1e9; sy = (h / 2) / abs(dy) if dy else 1e9; s = min(sx, sy)
        return cx + dx * s, cy + dy * s
    def arrow(self, a, b, kind='assoc', label=None, dash=False, color=(30, 30, 30)):
        p1 = self.edge(a, self.center(b)); p2 = self.edge(b, self.center(a))
        d = self.d
        if dash:
            L = math.hypot(p2[0] - p1[0], p2[1] - p1[1]); n = int(L // 10)
            for i in range(0, n, 2):
                s = i / n; e = min((i + 1) / n, 1)
                d.line([p1[0] + (p2[0] - p1[0]) * s, p1[1] + (p2[1] - p1[1]) * s, p1[0] + (p2[0] - p1[0]) * e, p1[1] + (p2[1] - p1[1]) * e], fill=color, width=2)
        else:
            d.line([p1, p2], fill=color, width=2)
        ang = math.atan2(p2[1] - p1[1], p2[0] - p1[0]); L = 14
        pts = [p2, (p2[0] - L * math.cos(ang - 0.4), p2[1] - L * math.sin(ang - 0.4)), (p2[0] - L * math.cos(ang + 0.4), p2[1] - L * math.sin(ang + 0.4))]
        if kind == 'inherit': d.polygon(pts, fill='white', outline=color)
        else: d.polygon(pts, fill=color)
        if label:
            mx, my = (p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2
            d.text((mx + 4, my - 14), label, font=F(11), fill=(120, 0, 0))
    def save(self, path): self.im.save(path)

CHK = (253, 235, 208); UNC = (221, 240, 221); ERR = (248, 215, 218); BASE = (232, 240, 252); CUST = (255, 250, 205)

# ---------- Q1: Throwable hierarchy for the ATM ----------
c = Canvas(1500, 640, 'Q1 - Throwable hierarchy relevant to the ATM (orange = checked, green = unchecked, red = Error, yellow = ATM user-defined)')
th = c.box(620, 50, 240, 40, 'Throwable', fill=BASE)
er = c.box(130, 150, 200, 40, 'Error', fill=ERR)
ex = c.box(780, 150, 200, 40, 'Exception', fill=CHK)
c.arrow(er, th, 'inherit'); c.arrow(ex, th, 'inherit')
so = c.box(20, 260, 230, 40, 'StackOverflowError', fill=ERR); oom = c.box(270, 260, 230, 40, 'OutOfMemoryError', fill=ERR)
c.arrow(so, er, 'inherit'); c.arrow(oom, er, 'inherit')
io = c.box(560, 260, 180, 40, 'IOException', fill=CHK); ae = c.box(790, 260, 200, 40, 'AtmException', fill=CUST); rt = c.box(1160, 260, 260, 40, 'RuntimeException', fill=UNC)
for b in (io, ae, rt): c.arrow(b, ex, 'inherit')
fnf = c.box(540, 380, 220, 40, 'FileNotFoundException', fill=CHK); c.arrow(fnf, io, 'inherit')
ins = c.box(680, 380, 0, 0, '') if False else None
isf = c.box(790, 380, 250, 50, 'InsufficientFundsException', fill=CUST); anf = c.box(790, 450, 250, 50, 'AccountNotFoundException', fill=CUST)
c.arrow(isf, ae, 'inherit'); c.arrow(anf, ae, 'inherit')
iae = c.box(1090, 380, 240, 40, 'IllegalArgumentException', fill=UNC); nse = c.box(1350, 380, 140, 40, 'NoSuchElement', fill=UNC)
c.arrow(iae, rt, 'inherit'); c.arrow(nse, rt, 'inherit')
nfe = c.box(1090, 500, 240, 40, 'NumberFormatException', fill=UNC); ime = c.box(1330, 500, 160, 40, 'InputMismatch', fill=UNC)
c.arrow(nfe, iae, 'inherit'); c.arrow(ime, nse, 'inherit')
c.d.text((20, 580), 'Checked: the compiler forces catch or throws.  Unchecked: RuntimeException and Error subclasses.  Errors are never caught by the ATM.', font=F(14), fill='black')
c.d.text((20, 604), 'InputMismatchException (Scanner.nextInt) and NumberFormatException (Integer.parseInt) are unrelated, so they may share one multi-catch; InputMismatch IS-A NoSuchElement, so the two cannot.', font=F(13), fill='black')
c.save('Q1/throwable_hierarchy.png')

# ---------- Q2: exception hierarchy + propagation ----------
c = Canvas(1340, 520, 'Q2 - Class diagram of the portal exceptions (all checked, extend Exception)')
e = c.box(30, 60, 200, 40, 'Exception', fill=BASE)
ee = c.box(30, 160, 330, 90, 'ExamException', ['+ ExamException(String message)', '+ ExamException(String message, Throwable cause)'], fill=CHK)
c.arrow(ee, e, 'inherit')
subs = [('ExamClosedException', ['course, closedAt'], 420), ('NotRegisteredException', ['student, course'], 640), ('FileTooLargeException', ['sizeKb, limitKb'], 860), ('SubmissionStorageException', ['message, cause (IOException)'], 1090)]
for t, l, x in subs:
    b = c.box(x - 20, 330, 200 if x < 1000 else 235, 70, t, l, fill=CUST if 'Storage' in t else CHK)
    c.arrow(b, ee, 'inherit')
r = c.box(420, 60, 230, 40, 'RuntimeException', fill=UNC)
ia = c.box(700, 60, 330, 40, 'IllegalArgumentException (unchecked)', fill=UNC)
c.arrow(ia, r, 'inherit')
c.d.text((420, 120), 'unchecked: caller bug (blank student id)', font=F(13), fill=(0, 100, 0))
c.d.text((20, 440), 'Business rules (closed, not registered, too large) are checked: the controller can recover and tell the student exactly why.', font=F(14), fill='black')
c.d.text((20, 464), 'SubmissionStorageException wraps the IOException of the storage layer (technical failure) and keeps it as the cause.', font=F(14), fill='black')
c.save('Q2/exception_hierarchy.png')

c = Canvas(1300, 420, 'Q2 - Propagation path of SubmissionStorageException (storage layer to controller)')
s1 = c.box(40, 100, 250, 120, 'Storage.writeToDisk()', ['throws IOException', '"Disk quota exceeded"'], fill=ERR)
s2 = c.box(360, 100, 250, 120, 'Storage.save()', ['catch (IOException e)', 'throw new SubmissionStorage', 'Exception(msg, e)  <- cause kept'], fill=CUST)
s3 = c.box(680, 100, 250, 120, 'ExamService.submit()', ['declares throws ExamException', 'no catch: lets it travel up'], fill=BASE)
s4 = c.box(1000, 100, 260, 120, 'Controller.handle()', ['catch (SubmissionStorage', 'Exception e): FINAL HANDLER', 'logs the cause chain'], fill=UNC)
for a, b in ((s1, s2), (s2, s3), (s3, s4)): c.arrow(a, b, 'assoc')
c.d.text((40, 250), 'The student sees:  "We could not save your answer. Please try again in a few minutes."', font=F(16, True), fill=(0, 90, 0))
c.d.text((40, 285), 'The log keeps:     SubmissionStorageException: Could not store a2.pdf for S1  <-  Caused by: java.io.IOException: Disk quota exceeded', font=M(13), fill='black')
c.d.text((40, 320), 'Stack unwinding order: writeToDisk -> save -> submit -> handle (each frame is left until a matching catch is found).', font=F(14), fill='black')
c.save('Q2/propagation_path.png')

# ---------- Q4: thread life cycle ----------
c = Canvas(1350, 560, 'Q4 - Thread life-cycle states (blue = states a scanner station passes through in the program)')
new = c.box(40, 220, 130, 60, 'NEW', ['Thread created'], fill=(200, 225, 250))
run = c.box(300, 220, 200, 60, 'RUNNABLE', ['ready or running (start())'], fill=(200, 225, 250))
tw = c.box(640, 60, 250, 70, 'TIMED_WAITING', ['sleep(ms), join(ms), wait(ms)'], fill=(200, 225, 250))
wt = c.box(640, 220, 250, 70, 'WAITING', ['join(), wait(), park()'], fill=(235, 235, 235))
bl = c.box(640, 380, 250, 70, 'BLOCKED', ['waiting for a monitor lock'], fill=(235, 235, 235))
te = c.box(1040, 220, 220, 60, 'TERMINATED', ['run() finished'], fill=(200, 225, 250))
c.arrow(new, run, 'assoc', 'start()'); c.arrow(run, tw, 'assoc', 'sleep()'); c.arrow(tw, run, 'assoc', None, dash=True)
c.arrow(run, wt, 'assoc', 'join() / wait()'); c.arrow(wt, run, 'assoc', None, dash=True)
c.arrow(run, bl, 'assoc', 'synchronized (lock busy)'); c.arrow(bl, run, 'assoc', None, dash=True)
c.arrow(run, te, 'assoc', 'run() returns')
c.d.text((20, 470), 'Dashed arrows return to RUNNABLE: sleep time over / notify or the joined thread ends / the monitor lock is acquired.', font=F(14), fill=(120,0,0))
c.d.text((20, 500), 'A scanner station: NEW (before start) -> RUNNABLE (after start) -> TIMED_WAITING (inside Thread.sleep while scanning) -> RUNNABLE -> TERMINATED.', font=F(14), fill='black')
c.d.text((20, 524), 'main thread: RUNNABLE -> WAITING while it is inside join() for a station.  Observed with getState() in LifeCycle.java.', font=F(14), fill='black')
c.save('Q4/thread_lifecycle.png')

# ---------- Q7: class diagram ----------
c = Canvas(1500, 760, 'Q7 - Class diagram of the concurrent result processor (Student A design)')
rp = c.box(560, 60, 360, 180, 'ResultProcessor', ['+ process(dir, ids, threads, latency): Summary', '- results: ConcurrentHashMap<String,Result>', '- grades: ConcurrentHashMap<String,LongAdder>', '- failures: Map<String,String>', '- pool: ExecutorService (fixed size p)', '- futures: List<Future<Result>>'], fill=BASE)
ex = c.box(60, 80, 300, 80, 'ExecutorService', ['<<interface>> submit(Callable)'], fill=(240, 240, 240))
fu = c.box(60, 220, 300, 80, 'Future<Result>', ['<<interface>> get(timeout)', 'throws ExecutionException'], fill=(240, 240, 240))
ca = c.box(1100, 80, 340, 80, 'Callable<Result>', ['<<interface>> call() throws Exception'], fill=(240, 240, 240))
rs = c.box(1100, 230, 340, 70, 'ScoreReader', ['+ read(dir, id, latency): Result throws ScoreFileException'], fill=BASE)
re = c.box(1100, 360, 340, 60, 'Result (record)', ['id, total, grade'], fill=BASE)
se = c.box(560, 380, 360, 70, 'ScoreFileException (checked)', ['+ (message), (message, cause)'], fill=CHK)
me = c.box(380, 540, 280, 60, 'MissingFileException', ['file does not exist'], fill=CHK)
co = c.box(820, 540, 300, 60, 'CorruptFileException', ['bad text, range, structure'], fill=CHK)
ex0 = c.box(560, 300, 360, 40, 'Exception', fill=BASE) if False else None
c.arrow(me, se, 'inherit'); c.arrow(co, se, 'inherit')
c.arrow(rp, ex, 'assoc', 'uses'); c.arrow(rp, fu, 'assoc', 'collects'); c.arrow(rp, ca, 'assoc', 'submits lambda'); c.arrow(ca, rs, 'assoc', 'calls')
c.arrow(rs, re, 'assoc', 'creates'); c.arrow(rs, se, 'assoc', 'throws', dash=True)
c.d.text((20, 640), 'Shared data: results (ConcurrentHashMap, putIfAbsent detects duplicates), grades (LongAdder counters, no contention on one variable), failures (ConcurrentHashMap).', font=F(14), fill='black')
c.d.text((20, 664), 'Failure design: a failing task never throws into the pool thread; the exception is captured in its Future. The main loop calls get() inside try/catch for EVERY future, classifies the cause and continues.', font=F(14), fill='black')
c.d.text((20, 688), 'Invariant checked at the end: ok + corrupt + missing + other = number of files.', font=F(14), fill='black')
c.save('Q7/class_diagram.png')
print('diagrams ok')
