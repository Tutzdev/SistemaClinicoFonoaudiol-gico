import {
  createContext,
  useContext,
  useEffect,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import {
  Link,
  Navigate,
  NavLink,
  Outlet,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from "react-router-dom";
import {
  ArrowLeft,
  ArrowUpRight,
  CalendarDays,
  CircleUserRound,
  LayoutDashboard,
  LogOut,
  Menu,
  Settings2,
  ShieldCheck,
  Stethoscope,
  Users,
  X,
} from "lucide-react";
import { api, ApiError, resetCsrf } from "../api";
import {
  Brand,
  Button,
  ErrorNotice,
  Field,
  Loading,
  Modal,
  Notice,
} from "../components/ui";
import type { Session } from "../types";
import { CrudPage } from "./CrudPage";
import { AgendaPage, Dashboard } from "./AgendaPage";
import { SettingsPage } from "./SettingsPage";

const AuthContext = createContext<{
  user: Session;
  refresh: () => Promise<void>;
} | null>(null);
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("Sessão indisponível");
  return ctx;
}

function Login({
  onLogin,
  expired,
}: {
  onLogin: (user: Session) => void;
  expired: boolean;
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const navigate = useNavigate();
  useEffect(() => {
    document.title = "Acesso da equipe · Espaço Sinapse";
  }, []);
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api("/auth/login", "POST", { email, password });
      resetCsrf();
      const user = await api<Session>("/auth/me");
      onLogin(user);
      navigate("/admin", { replace: true });
    } catch (err) {
      setError(err as Error);
    } finally {
      setBusy(false);
    }
  }
  return (
    <main className="login-page">
      <section className="login-aside">
        <Link to="/" aria-label="Espaço Sinapse, início">
          <Brand light />
        </Link>
        <div>
          <p className="eyebrow">ESPAÇO DA EQUIPE</p>
          <p className="display-heading">
            Organização para
            <br />
            cuidar de cada
            <br />
            <span>encontro.</span>
          </p>
          <p>
            Pacientes, profissionais e agenda
            <br />
            em um só lugar.
          </p>
        </div>
        <p className="login-aside-note">Espaço Sinapse · Fonoaudiologia</p>
      </section>
      <section className="login-main">
        <Link to="/" className="text-link back-site">
          <ArrowLeft size={17} aria-hidden />
          Voltar para o site
        </Link>
        <div className="login-form-wrap">
          <span className="login-icon">
            <ShieldCheck size={26} aria-hidden />
          </span>
          <h1>Acesso da equipe</h1>
          <p>Entre com sua conta para acessar a clínica.</p>
          {expired ? (
            <Notice>Sua sessão expirou. Entre novamente para continuar.</Notice>
          ) : null}
          <form onSubmit={submit} className="form-stack">
            <Field
              label="E-mail"
              type="email"
              autoComplete="username"
              value={email}
              required
              onChange={(e) => setEmail(e.target.value)}
              error={error instanceof ApiError ? error.fields.email : undefined}
            />
            <Field
              label="Senha"
              type="password"
              autoComplete="current-password"
              value={password}
              required
              onChange={(e) => setPassword(e.target.value)}
            />
            <ErrorNotice error={error} />
            <Button type="submit" busy={busy}>
              Entrar na clínica <ArrowUpRight size={18} aria-hidden />
            </Button>
          </form>
          <p className="login-help">
            Precisa de acesso? Fale com a pessoa responsável pela administração
            da clínica.
          </p>
        </div>
        <small className="login-copyright">
          Acesso exclusivo para a equipe autorizada.
        </small>
      </section>
    </main>
  );
}
function PasswordModal({ onClose }: { onClose: () => void }) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [done, setDone] = useState(false);
  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (newPassword !== confirmation) {
      setError(new Error("A confirmação deve ser igual à nova senha."));
      return;
    }
    setBusy(true);
    try {
      await api("/auth/password", "PUT", { currentPassword, newPassword });
      setDone(true);
    } catch (err) {
      setError(err as Error);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title="Alterar senha"
      description="Use uma senha exclusiva para sua conta."
      onClose={() => {
        if (done) window.location.assign("/login");
        else onClose();
      }}
    >
      {done ? (
        <>
          <Notice>
            Senha alterada com sucesso. Entre novamente com sua nova senha.
          </Notice>
          <div className="form-actions">
            <Button onClick={() => window.location.assign("/login")}>
              Voltar ao login
            </Button>
          </div>
        </>
      ) : (
        <form onSubmit={submit} className="form-stack">
          <Field
            label="Senha atual"
            type="password"
            autoComplete="current-password"
            required
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
          />
          <Field
            label="Nova senha"
            type="password"
            autoComplete="new-password"
            minLength={12}
            required
            hint="Use ao menos 12 caracteres."
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
          />
          <Field
            label="Confirmar nova senha"
            type="password"
            autoComplete="new-password"
            required
            value={confirmation}
            onChange={(e) => setConfirmation(e.target.value)}
          />
          <ErrorNotice error={error} />
          <div className="form-actions">
            <Button variant="secondary" type="button" onClick={onClose}>
              Cancelar
            </Button>
            <Button busy={busy}>Salvar senha</Button>
          </div>
        </form>
      )}
    </Modal>
  );
}
function AdminLayout({ onLogout }: { onLogout: () => void }) {
  const { user } = useAuth();
  const [menu, setMenu] = useState(false);
  const [password, setPassword] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [loggingOut, setLoggingOut] = useState(false);
  const location = useLocation();
  useEffect(() => {
    setMenu(false);
  }, [location.pathname]);
  const nav = [
    { path: "/admin", label: "Visão geral", icon: LayoutDashboard },
    { path: "/admin/agenda", label: "Agenda", icon: CalendarDays },
    { path: "/admin/pacientes", label: "Pacientes", icon: Users },
    { path: "/admin/profissionais", label: "Profissionais", icon: Stethoscope },
    { path: "/admin/servicos", label: "Serviços", icon: CircleUserRound },
    ...(user.role === "ADMIN"
      ? [
          {
            path: "/admin/usuarios",
            label: "Equipe e acessos",
            icon: ShieldCheck,
          },
          {
            path: "/admin/configuracoes",
            label: "Configurações",
            icon: Settings2,
          },
        ]
      : []),
  ];
  async function logout() {
    setLoggingOut(true);
    setError(null);
    try {
      await api("/auth/logout", "POST");
      resetCsrf();
      onLogout();
    } catch (err) {
      setError(err as Error);
    } finally {
      setLoggingOut(false);
    }
  }
  return (
    <div className="admin-layout">
      <a className="skip-link" href="#admin-content">
        Ir para o conteúdo
      </a>
      <header className="admin-mobile-header">
        <Link to="/admin">
          <Brand compact />
        </Link>
        <Button
          className="icon-button"
          variant="ghost"
          aria-expanded={menu}
          aria-controls="admin-sidebar"
          aria-label={menu ? "Fechar navegação" : "Abrir navegação"}
          onClick={() => setMenu((v) => !v)}
        >
          {menu ? <X size={22} aria-hidden /> : <Menu size={22} aria-hidden />}
        </Button>
      </header>
      <aside
        className={`admin-sidebar ${menu ? "sidebar-open" : ""}`}
        id="admin-sidebar"
        aria-label="Navegação e conta da equipe"
      >
        <Link to="/admin" className="sidebar-brand">
          <Brand compact />
        </Link>
        <span className="sidebar-caption">GESTÃO DA CLÍNICA</span>
        <nav aria-label="Navegação da equipe">
          {nav.map((item) => (
            <NavLink
              end={item.path === "/admin"}
              to={item.path}
              key={item.path}
            >
              <item.icon size={19} aria-hidden />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <Link to="/" className="view-site" target="_blank" rel="noopener">
            Ver site público <ArrowUpRight size={16} aria-hidden />
          </Link>
          <button
            className="user-summary"
            onClick={() => setPassword(true)}
            aria-label={`Conta de ${user.name}. Alterar senha`}
          >
            <span className="avatar">
              {user.name.slice(0, 2).toUpperCase()}
            </span>
            <span>
              <strong>{user.name}</strong>
              <small>
                {user.role === "ADMIN" ? "Administrador" : "Recepção"}
              </small>
            </span>
          </button>
          <Button variant="ghost" busy={loggingOut} onClick={logout}>
            <LogOut size={17} aria-hidden />
            Sair
          </Button>
        </div>
      </aside>
      <main id="admin-content" className="admin-main">
        <div className="admin-topbar">
          <span>
            Espaço Sinapse <span className="topbar-divider">/</span> Gestão
          </span>
          <span className="topbar-role">
            <span className="eyebrow-dot" />
            {user.role === "ADMIN" ? "Administrador" : "Recepção"}
          </span>
        </div>
        <div className="admin-content">
          <ErrorNotice error={error} />
          <Outlet />
        </div>
        <footer className="admin-footer">
          Espaço Sinapse <span>Horários de Brasília · America/Sao_Paulo</span>
        </footer>
      </main>
      {password ? <PasswordModal onClose={() => setPassword(false)} /> : null}
    </div>
  );
}
function AdminOnly({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  return user.role === "ADMIN" ? (
    children
  ) : (
    <div className="empty">
      <h1>Acesso restrito</h1>
      <p>Esta área está disponível para a administração da clínica.</p>
      <Link className="button button-primary" to="/admin">
        Voltar à visão geral
      </Link>
    </div>
  );
}
export default function AdminApp() {
  const [user, setUser] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);
  const [expired, setExpired] = useState(false);
  const [connectionError, setConnectionError] = useState<Error | null>(null);
  async function refresh() {
    try {
      setConnectionError(null);
      setUser(await api<Session>("/auth/me"));
    } catch (err) {
      if (!(err instanceof ApiError && err.status === 401))
        setConnectionError(err as Error);
      setUser(null);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    void refresh();
    const expire = () => {
      setUser(null);
      setExpired(true);
      resetCsrf();
    };
    window.addEventListener("session-expired", expire);
    return () => window.removeEventListener("session-expired", expire);
  }, []);
  if (loading) return <Loading full />;
  if (connectionError)
    return (
      <main className="connection-failure">
        <Brand />
        <h1>Não foi possível acessar a clínica</h1>
        <ErrorNotice error={connectionError} onRetry={() => void refresh()} />
        <Link to="/">Voltar ao site</Link>
      </main>
    );
  return (
    <Routes>
      <Route
        path="/login"
        element={
          user ? (
            <Navigate replace to="/admin" />
          ) : (
            <Login
              expired={expired}
              onLogin={(u) => {
                setUser(u);
                setExpired(false);
              }}
            />
          )
        }
      />
      {user ? (
        <Route
          path="/admin"
          element={
            <AuthContext.Provider value={{ user, refresh }}>
              <AdminLayout onLogout={() => setUser(null)} />
            </AuthContext.Provider>
          }
        >
          <Route index element={<Dashboard />} />
          <Route path="agenda" element={<AgendaPage />} />
          <Route path="pacientes" element={<CrudPage kind="patients" />} />
          <Route
            path="profissionais"
            element={<CrudPage kind="professionals" />}
          />
          <Route path="servicos" element={<CrudPage kind="services" />} />
          <Route
            path="usuarios"
            element={
              <AdminOnly>
                <CrudPage kind="users" />
              </AdminOnly>
            }
          />
          <Route
            path="configuracoes"
            element={
              <AdminOnly>
                <SettingsPage />
              </AdminOnly>
            }
          />
          <Route path="*" element={<Navigate to="/admin" replace />} />
        </Route>
      ) : (
        <Route path="/admin/*" element={<Navigate to="/login" replace />} />
      )}
      <Route
        path="*"
        element={
          <main className="empty">
            <h1>Página não encontrada</h1>
            <Link to="/">Voltar ao início</Link>
          </main>
        }
      />
    </Routes>
  );
}
