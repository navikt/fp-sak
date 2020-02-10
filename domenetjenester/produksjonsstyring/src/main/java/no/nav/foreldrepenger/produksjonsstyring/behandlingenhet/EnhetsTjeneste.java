package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.produksjonsstyring.arbeidsfordeling.ArbeidsfordelingTjeneste;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
public class EnhetsTjeneste {

    private TpsTjeneste tpsTjeneste;
    private ArbeidsfordelingTjeneste arbeidsfordelingTjeneste;

    private LocalDate sisteInnhenting = LocalDate.MIN;
    // Produksjonsstyring skjer på nivå TEMA - behandlingTema ikke hensyntatt in NORG2
    private OrganisasjonsEnhet enhetKode6;
    private OrganisasjonsEnhet enhetKlage;
    private List<OrganisasjonsEnhet> alleBehandlendeEnheter;

    public EnhetsTjeneste() {
        // For CDI proxy
    }

    @Inject
    public EnhetsTjeneste(TpsTjeneste tpsTjeneste, ArbeidsfordelingTjeneste arbeidsfordelingTjeneste) {
        this.tpsTjeneste = tpsTjeneste;
        this.arbeidsfordelingTjeneste = arbeidsfordelingTjeneste;
    }


    List<OrganisasjonsEnhet> hentEnhetListe() {
        oppdaterEnhetCache();
        return alleBehandlendeEnheter;
    }

    OrganisasjonsEnhet hentEnhetSjekkRegistrerteRelasjoner(AktørId aktørId, BehandlingTema behandlingTema) {
        oppdaterEnhetCache();
        PersonIdent fnr = tpsTjeneste.hentFnrForAktør(aktørId);

        GeografiskTilknytning geografiskTilknytning = tpsTjeneste.hentGeografiskTilknytning(fnr);
        String aktivDiskresjonskode = geografiskTilknytning.getDiskresjonskode();
        if (!Diskresjonskode.KODE6.getKode().equals(aktivDiskresjonskode)) {
            boolean relasjonMedK6 = tpsTjeneste.hentDiskresjonskoderForFamilierelasjoner(fnr).stream()
                .anyMatch(geo -> Diskresjonskode.KODE6.getKode().equals(geo.getDiskresjonskode()));
            if (relasjonMedK6) {
                aktivDiskresjonskode = Diskresjonskode.KODE6.getKode();
            }
        }

        return arbeidsfordelingTjeneste.finnBehandlendeEnhet(geografiskTilknytning.getTilknytning(), aktivDiskresjonskode, behandlingTema);
    }

    Optional<OrganisasjonsEnhet> oppdaterEnhetSjekkOppgitte(String enhetId, List<AktørId> relaterteAktører) {
        oppdaterEnhetCache();
        if (enhetKode6.getEnhetId().equals(enhetId) || enhetKlage.getEnhetId().equals(enhetId)) {
            return Optional.empty();
        }

        return sjekkSpesifiserteRelaterte(relaterteAktører);
    }

    Optional<OrganisasjonsEnhet> oppdaterEnhetSjekkRegistrerteRelasjoner(String enhetId, BehandlingTema behandlingTema, AktørId aktørId, Optional<AktørId> kobletAktørId, List<AktørId> relaterteAktører) {
        oppdaterEnhetCache();
        if (enhetKode6.getEnhetId().equals(enhetId) || enhetKlage.getEnhetId().equals(enhetId)) {
            return Optional.empty();
        }

        OrganisasjonsEnhet enhet = hentEnhetSjekkRegistrerteRelasjoner(aktørId, behandlingTema);
        if (enhetKode6.getEnhetId().equals(enhet.getEnhetId())) {
            return Optional.of(enhetKode6);
        }
        if (kobletAktørId.isPresent()) {
            OrganisasjonsEnhet enhetKoblet = hentEnhetSjekkRegistrerteRelasjoner(kobletAktørId.get(), behandlingTema);
            if (enhetKode6.getEnhetId().equals(enhetKoblet.getEnhetId())) {
                return Optional.of(enhetKode6);
            }
        }
        if (sjekkSpesifiserteRelaterte(relaterteAktører).isPresent()) {
            return Optional.of(enhetKode6);
        }
        if (!gyldigEnhetId(enhetId)) {
            return Optional.of(enhet);
        }

        return Optional.empty();
    }

    private Optional<OrganisasjonsEnhet> sjekkSpesifiserteRelaterte(List<AktørId> relaterteAktører) {
        for (AktørId relatert : relaterteAktører) {
            PersonIdent personIdent = tpsTjeneste.hentFnrForAktør(relatert);
            GeografiskTilknytning geo = tpsTjeneste.hentGeografiskTilknytning(personIdent);
            if (Diskresjonskode.KODE6.getKode().equals(geo.getDiskresjonskode())) {
                return Optional.of(enhetKode6);
            }
        }
        return Optional.empty();
    }

    private void oppdaterEnhetCache() {
        if (sisteInnhenting.isBefore(FPDateUtil.iDag())) {
            enhetKode6 = arbeidsfordelingTjeneste.hentEnhetForDiskresjonskode(Diskresjonskode.KODE6.getKode(), BehandlingTema.UDEFINERT);
            enhetKlage = arbeidsfordelingTjeneste.getKlageInstansEnhet();
            alleBehandlendeEnheter = arbeidsfordelingTjeneste.finnAlleBehandlendeEnhetListe(BehandlingTema.UDEFINERT);
            sisteInnhenting = FPDateUtil.iDag();
        }
    }

    private boolean gyldigEnhetId(String enhetId) {
        return finnOrganisasjonsEnhet(enhetId).isPresent();
    }

    Optional<OrganisasjonsEnhet> finnOrganisasjonsEnhet(String enhetId) {
        oppdaterEnhetCache();
        return alleBehandlendeEnheter.stream().filter(e -> enhetId.equals(e.getEnhetId())).findFirst();
    }

    OrganisasjonsEnhet enhetsPresedens(OrganisasjonsEnhet enhetSak1, OrganisasjonsEnhet enhetSak2, boolean arverKlage) {
        oppdaterEnhetCache();
        if (arverKlage && enhetKlage.getEnhetId().equals(enhetSak1.getEnhetId())) {
            return enhetSak1;
        }
        if (enhetKode6.getEnhetId().equals(enhetSak1.getEnhetId()) || enhetKode6.getEnhetId().equals(enhetSak2.getEnhetId())) {
            return enhetKode6;
        }
        return enhetSak1;
    }

    OrganisasjonsEnhet getEnhetKlage() {
        oppdaterEnhetCache();
        return enhetKlage;
    }

}
