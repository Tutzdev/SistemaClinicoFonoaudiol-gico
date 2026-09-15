import {
  ArrowDown,
  ArrowRight,
  ArrowUpRight,
  Mail,
  MapPin,
  Menu,
  MessageCircle,
  Phone,
  X,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useResource } from "../api";
import { Brand, Button, ErrorNotice, Loading } from "../components/ui";
import { phoneUri, whatsapp } from "../format";
import type { Clinic, Professional, Service } from "../types";

function ConnectionArt() {
  return (
    <div className="connection-art" aria-hidden="true">
      <div className="art-coordinate art-coordinate-top">
        ESCUTA · EXPRESSÃO · CONEXÃO
      </div>
      <svg viewBox="0 0 560 540" fill="none">
        <circle
          cx="280"
          cy="270"
          r="224"
          stroke="currentColor"
          strokeOpacity=".14"
        />
        <circle
          cx="280"
          cy="270"
          r="176"
          stroke="currentColor"
          strokeOpacity=".12"
        />
        <path
          d="M134 283c0-103 67-165 164-165h125v93H302c-53 0-76 26-76 74v132h-92V283Z"
          fill="#AEC8BE"
        />
        <path
          d="M428 257c0 103-67 165-164 165H139v-93h121c53 0 76-26 76-74V123h92v134Z"
          fill="#176B64"
        />
        <path
          d="M226 329h34c53 0 76-26 76-74v-44h92v46c0 103-67 165-164 165h-38v-93Z"
          fill="#173B3B"
        />
        <path d="M337 211h87v-88h-87v88Z" fill="#D9A56B" />
        <circle cx="80" cy="270" r="5" fill="#176B64" />
        <circle cx="480" cy="270" r="5" fill="#D9A56B" />
        <path
          d="M280 27v22M280 491v22M37 270h22M502 270h22"
          stroke="currentColor"
          strokeOpacity=".5"
        />
      </svg>
      <div className="art-caption">
        <span className="tiny-line" />
        Cada conexão começa com espaço.
      </div>
    </div>
  );
}
export function PublicSite() {
  const clinic = useResource<Clinic>("/public/clinic");
  const services = useResource<Service[]>("/public/services");
  const professionals = useResource<Professional[]>("/public/professionals");
  const [menu, setMenu] = useState(false);
  const c = clinic.data;
  useEffect(() => {
    document.title = `${c?.displayName || "Espaço Sinapse"} · Fonoaudiologia`;
  }, [c?.displayName]);
  const contactLink = c?.whatsapp ? whatsapp(c.whatsapp) : "#contato";
  const closeMenu = () => setMenu(false);
  return (
    <div className="public-site">
      <a className="skip-link" href="#conteudo">
        Ir para o conteúdo
      </a>
      <header className="public-header">
        <div className="public-container header-inner">
          <Link
            to="/"
            aria-label={`${c?.displayName || "Espaço Sinapse"}, início`}
          >
            <Brand name={c?.displayName} />
          </Link>
          <Button
            className="mobile-menu icon-button"
            variant="ghost"
            aria-label={menu ? "Fechar menu" : "Abrir menu"}
            aria-expanded={menu}
            aria-controls="public-nav"
            onClick={() => setMenu((v) => !v)}
          >
            {menu ? (
              <X aria-hidden size={22} />
            ) : (
              <Menu aria-hidden size={22} />
            )}
          </Button>
          <nav
            id="public-nav"
            className={`public-nav ${menu ? "is-open" : ""}`}
            aria-label="Navegação principal"
          >
            <a href="#sobre" onClick={closeMenu}>
              O espaço
            </a>
            {services.data?.length ? (
              <a href="#servicos" onClick={closeMenu}>
                Atendimento
              </a>
            ) : null}
            {professionals.data?.length ? (
              <a href="#equipe" onClick={closeMenu}>
                Profissionais
              </a>
            ) : null}
            <a href="#contato" onClick={closeMenu}>
              Contato
            </a>
            <a
              className="button button-primary header-cta"
              href={contactLink}
              onClick={closeMenu}
            >
              Falar com a clínica <ArrowUpRight size={17} aria-hidden />
            </a>
          </nav>
        </div>
      </header>
      <main id="conteudo">
        <section className="hero public-container" aria-labelledby="hero-title">
          <div className="hero-copy">
            <p className="eyebrow">
              <span className="eyebrow-dot" />
              {(c?.displayName || "Espaço Sinapse").toLocaleUpperCase(
                "pt-BR",
              )}{" "}
              · FONOAUDIOLOGIA
            </p>
            <h1 id="hero-title">
              Um espaço
              <br />
              para a <span>comunicação.</span>
            </h1>
            <p className="hero-description">
              {c?.description ||
                "Conheça o Espaço Sinapse e entre em contato para informações sobre atendimento em fonoaudiologia."}
            </p>
            <div className="hero-actions">
              <a
                className="button button-primary button-large"
                href={contactLink}
              >
                <MessageCircle size={20} aria-hidden />
                Conversar pelo WhatsApp
                <ArrowUpRight size={18} aria-hidden />
              </a>
              <a className="text-link" href="#contato">
                Outras formas de contato <ArrowRight size={17} aria-hidden />
              </a>
            </div>
            <a className="hero-scroll" href="#sobre">
              <span>
                <ArrowDown size={17} aria-hidden />
              </span>
              Conheça o espaço
            </a>
          </div>
          <ConnectionArt />
        </section>
        <section className="about-section" id="sobre">
          <div className="public-container about-layout">
            <div>
              <p className="eyebrow">
                {(c?.displayName || "Espaço Sinapse").toLocaleUpperCase(
                  "pt-BR",
                )}
              </p>
              <h2>
                Uma conversa.
                <br />
                Um primeiro passo.
              </h2>
            </div>
            <div className="about-copy">
              <p>
                Buscar informações também faz parte do cuidado. Aqui, você
                encontra um caminho direto para conversar com a clínica e
                conhecer as possibilidades de atendimento.
              </p>
              <p>
                Fale conosco para esclarecer suas dúvidas e consultar a
                disponibilidade para agendamento.
              </p>
              <a className="text-link" href="#contato">
                Encontre a melhor forma de falar conosco{" "}
                <ArrowRight size={18} aria-hidden />
              </a>
            </div>
          </div>
        </section>
        {services.data?.length ? (
          <section id="servicos" className="public-container public-section">
            <div className="section-heading">
              <div>
                <p className="eyebrow">ATENDIMENTO</p>
                <h2>Conheça os serviços.</h2>
              </div>
              <a href={contactLink} className="text-link">
                Consultar disponibilidade <ArrowUpRight size={18} aria-hidden />
              </a>
            </div>
            <div className="public-service-list">
              {services.data.map((service) => (
                <article key={service.id}>
                  <div>
                    <h3>{service.name}</h3>
                    <p>{service.description}</p>
                  </div>
                  <a
                    href={contactLink}
                    aria-label={`Consultar informações sobre ${service.name}`}
                  >
                    <ArrowUpRight aria-hidden size={25} />
                  </a>
                </article>
              ))}
            </div>
          </section>
        ) : null}
        {professionals.data?.length ? (
          <section id="equipe" className="public-container public-section">
            <p className="eyebrow">PROFISSIONAIS</p>
            <h2>Quem faz parte do espaço.</h2>
            <div className="public-team-list">
              {professionals.data.map((person) => (
                <article key={person.id}>
                  <div className="person-monogram" aria-hidden="true">
                    {person.name
                      .split(" ")
                      .slice(0, 2)
                      .map((n) => n[0])
                      .join("")}
                  </div>
                  <h3>{person.name}</h3>
                  {person.registration ? (
                    <p className="professional-registration">
                      CRFa {person.region} · {person.registration}
                    </p>
                  ) : null}
                  <p>{person.bio}</p>
                </article>
              ))}
            </div>
          </section>
        ) : null}
        <section id="contato" className="public-container contact-section">
          <div className="contact-intro">
            <p className="eyebrow">VAMOS CONVERSAR</p>
            <h2>
              O próximo passo
              <br />
              pode ser uma
              <br />
              <span>mensagem.</span>
            </h2>
            <p>
              Entre em contato para informações sobre atendimento e agendamento.
            </p>
            <small>
              A disponibilidade e a confirmação da consulta são combinadas
              diretamente com a clínica.
            </small>
          </div>
          <div className="contact-methods">
            {clinic.loading ? (
              <Loading />
            ) : clinic.error ? (
              <ErrorNotice error={clinic.error} onRetry={clinic.reload} />
            ) : c ? (
              <>
                {c.whatsapp ? (
                  <a className="contact-row" href={whatsapp(c.whatsapp)}>
                    <span className="contact-icon">
                      <MessageCircle size={22} aria-hidden />
                    </span>
                    <span>
                      <small>POR MENSAGEM</small>
                      <strong>Conversar pelo WhatsApp</strong>
                    </span>
                    <ArrowUpRight size={22} aria-hidden />
                  </a>
                ) : null}
                {c.phone ? (
                  <a className="contact-row" href={`tel:${phoneUri(c.phone)}`}>
                    <span className="contact-icon">
                      <Phone size={22} aria-hidden />
                    </span>
                    <span>
                      <small>POR TELEFONE</small>
                      <strong>{c.phone}</strong>
                    </span>
                    <ArrowUpRight size={22} aria-hidden />
                  </a>
                ) : null}
                {c.email ? (
                  <a className="contact-row" href={`mailto:${c.email}`}>
                    <span className="contact-icon">
                      <Mail size={22} aria-hidden />
                    </span>
                    <span>
                      <small>POR E-MAIL</small>
                      <strong>{c.email}</strong>
                    </span>
                    <ArrowUpRight size={22} aria-hidden />
                  </a>
                ) : null}
                {c.addressConfirmed && c.publicAddress ? (
                  <div className="contact-row">
                    <span className="contact-icon">
                      <MapPin size={22} aria-hidden />
                    </span>
                    <span>
                      <small>ONDE ESTAMOS</small>
                      <strong>{c.publicAddress}</strong>
                    </span>
                  </div>
                ) : null}
              </>
            ) : null}
          </div>
        </section>
      </main>
      <footer className="public-footer">
        <div className="public-container">
          <div className="footer-top">
            <Link to="/" aria-label="Voltar ao início">
              <Brand light name={c?.displayName} />
            </Link>
            <span>
              Espaço para escutar.
              <br />
              Espaço para se expressar.
            </span>
          </div>
          <div className="footer-bottom">
            <p>
              {c?.legalName || "ESPAÇO SINAPSE CLÍNICA DE FONOAUDIOLOGIA LTDA."}
              <br />
              CNPJ {c?.cnpj || "65.952.229/0001-71"}
            </p>
            <Link to="/login">
              Acesso da equipe <ArrowUpRight size={14} aria-hidden />
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
