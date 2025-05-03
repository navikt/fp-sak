package no.nav.foreldrepenger.tilganger;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.integrasjon.ansatt.AbstractAnsattInfoKlient;
import no.nav.vedtak.felles.integrasjon.ansatt.AnsattInfoDto;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPTILGANG)
public class AnsattInfoKlient extends AbstractAnsattInfoKlient {
    private static final Logger LOG = LoggerFactory.getLogger(AnsattInfoKlient.class);

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);
    private static final LRUCache<UUID, InnloggetNavAnsatt> ANSATT_CACHE = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);
    private static final LRUCache<UUID, Set<AnsattGruppe>> GRUPPE_CACHE = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);


    public InnloggetNavAnsattDto innloggetBruker() {
        var ansattOid = getCurrentAnsattOid();
        if (ansattOid == null) {
            return InnloggetNavAnsattDto.ukjentNavAnsatt("UGYLDIG", "Ukjent ansatt");
        }
        var ansatt = innloggetNavAnsatt(ansattOid);
        var grupper = medlemAvGrupper(ansattOid);
        return mapTilDomene(ansatt, grupper);
    }

    public boolean kanSaksbehandle() {
        var ansattOid = getCurrentAnsattOid();
        return medlemAvGrupper(ansattOid).contains(AnsattGruppe.SAKSBEHANDLER);
    }

    public boolean kanOverstyre() {
        var ansattOid = getCurrentAnsattOid();
        return medlemAvGrupper(ansattOid).contains(AnsattGruppe.OVERSTYRER);
    }

    public boolean medlemAvAnsattGruppe(AnsattGruppe gruppe) {
        var ansattOid = getCurrentAnsattOid();
        return medlemAvGrupper(ansattOid).contains(gruppe);
    }

    private InnloggetNavAnsatt innloggetNavAnsatt(UUID ansattOid) {
        var ansattInfo = ANSATT_CACHE.get(ansattOid);
        if (ansattInfo == null) {
            var før = System.currentTimeMillis();
            var ansatt = Optional.ofNullable(super.hentAnsattInfoForOid(ansattOid));
            ansattInfo = new InnloggetNavAnsatt(ansatt.map(AnsattInfoDto.Respons::ansattIdent).orElse("UGYLDIG"),
                ansatt.map(AnsattInfoDto.Respons::navn).orElse("Ukjent ansatt"));
            LOG.info("PROFIL Ansatt. Tilgang bruker profil oppslag: {}ms. ", System.currentTimeMillis() - før);
            ANSATT_CACHE.put(ansattOid, ansattInfo);
        }
        return ansattInfo;
    }

    private Set<AnsattGruppe> medlemAvGrupper(UUID ansattOid) {
        if (ansattOid == null) {
            return Set.of();
        }
        var grupper = GRUPPE_CACHE.get(ansattOid);
        if (grupper == null) {
            var før = System.currentTimeMillis();
            grupper = super.alleGrupper(ansattOid);
            LOG.info("PROFIL Grupper. Tilgang bruker profil oppslag: {}ms. ", System.currentTimeMillis() - før);
            GRUPPE_CACHE.put(ansattOid, grupper);
        }
        return grupper;
    }

    private UUID getCurrentAnsattOid() {
        return KontekstHolder.getKontekst() instanceof RequestKontekst rk ? rk.getOid() : null;
    }

    static InnloggetNavAnsattDto mapTilDomene(InnloggetNavAnsatt ansatt, Set<AnsattGruppe> grupper) {
        return new InnloggetNavAnsattDto.Builder(ansatt.brukernavn(), ansatt.navn())
            .kanSaksbehandle(grupper.contains(AnsattGruppe.SAKSBEHANDLER))
            .kanVeilede(grupper.contains(AnsattGruppe.VEILEDER))
            .kanOverstyre(grupper.contains(AnsattGruppe.OVERSTYRER))
            .kanOppgavestyre(grupper.contains(AnsattGruppe.OPPGAVESTYRER))
            .kanBehandleKode6(grupper.contains(AnsattGruppe.STRENGTFORTROLIG))
            .build();
    }
}
