package no.nav.foreldrepenger.behandling.steg.registrersøknad;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VENT_PÅ_SØKNAD;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.RegistrerFagsakEgenskaper;

@BehandlingStegRef(BehandlingStegType.REGISTRER_SØKNAD)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class RegistrerSøknadSteg implements BehandlingSteg {
    private static final Period VENT_PÅ_SØKNAD_PERIODE = Period.parse("P4W");
    private static final LocalDate FRIST_PRAKSIS_UTSETTELSE = LocalDate.of(2026, Month.APRIL, 20);
    private BehandlingRepository behandlingRepository;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private RegistrerFagsakEgenskaper registrerFagsakEgenskaper;
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;
    private Kompletthetsjekker kompletthetsjekker;

    RegistrerSøknadSteg() {
        // for CDI proxy
    }

    @Inject
    public RegistrerSøknadSteg(BehandlingRepository behandlingRepository,
                               MottatteDokumentTjeneste mottatteDokumentTjeneste,
                               RegistrerFagsakEgenskaper registrerFagsakEgenskaper,
                               HenleggBehandlingTjeneste henleggBehandlingTjeneste,
                               Kompletthetsjekker kompletthetsjekker) {
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.registrerFagsakEgenskaper = registrerFagsakEgenskaper;
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;
        this.kompletthetsjekker = kompletthetsjekker;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var mottatteDokumenterBehandling = mottatteDokumentTjeneste.hentMottatteDokument(kontekst.getBehandlingId());
        var alleDokumentSak = mottatteDokumentTjeneste.hentMottatteDokumentFagsak(kontekst.getFagsakId());
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        registrerFagsakEgenskaper.fagsakEgenskaperForBruker(behandling);

        if (alleDokumentSak.isEmpty()) {
            // Behandlingen er startet uten noe dokument, f.eks. gjennom en
            // forretningshendselse eller behov for utgående brev
            return resultatVedIngenMottatteDokument(behandling, kontekst.getSkriveLås());
        }

        var ref = BehandlingReferanse.fra(behandling);
        var søknadMottatt = kompletthetsjekker.vurderSøknadMottatt(ref);
        if (!søknadMottatt.erOppfylt()) {
            return evaluerSøknadMottattUoppfylt(behandling, kontekst.getSkriveLås(), søknadMottatt, VENT_PÅ_SØKNAD);
        }

        if (behandling.harNoenBehandlingÅrsaker(Set.of(BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE, BehandlingÅrsakType.FEIL_PRAKSIS_IVERKS_UTSET)) && !behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
            if (skalHenleggeBehandling(behandling)) {
                return henlegg(behandling, kontekst.getSkriveLås());
            }
            if (!behandling.harAksjonspunktMedType(VENT_PÅ_SØKNAD)) {
                var ventefrist = FRIST_PRAKSIS_UTSETTELSE.atStartOfDay();

                var aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(VENT_PÅ_SØKNAD, Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING, ventefrist);
                return BehandleStegResultat.utførtMedAksjonspunktResultat(aksjonspunktResultat);
            }
        }

        // OBS Dokumentmottak kan kopierere vedlegg fra tidligere behandlinger og disse
        // er "nyere" enn søknad som trigger ny 1gang/revurdering
        var nyesteSøknad = nyesteSøknad(mottatteDokumenterBehandling);
        if (nyesteSøknad == null && BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            // Må dekke tilfellet der det tidligere papirsøknad er avslått på
            // opplysningsplikt og det kommer ikke-søknad
            nyesteSøknad = nyesteSøknad(alleDokumentSak);
        }
        if (nyesteSøknad == null) {
            // Behandlingen er startet uten noe dokument, f.eks. gjennom en
            // forretningshendselse
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        return resultatVedNySøknad(behandling, nyesteSøknad);
    }

    private BehandleStegResultat resultatVedNySøknad(Behandling behandling, MottattDokument nyesteSøknad) {
        if (!nyesteSøknad.erUstrukturertDokument()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        var ytelseType = behandling.getFagsak().getYtelseType();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(ytelseType) && erUstrukturertEngangsstønadSøknad(nyesteSøknad)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD));
        }

        if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType) && erUstrukturertEndringSøknad(nyesteSøknad)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER));
        }

        if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType) && erUstrukturertForeldrepengerSøknad(nyesteSøknad)) {
            // Hvis vi mottar en førstegangssøknad på papir etter første vedtak, så skal det
            // tolkes som en endringssøknad
            if (behandling.erRevurdering()) {
                return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER));
            }
            return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(REGISTRER_PAPIRSØKNAD_FORELDREPENGER));
        }

        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType) && erUstrukturertSvangerskapspengerSøknad(nyesteSøknad)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private MottattDokument nyesteSøknad(List<MottattDokument> mottatteDokumenter) {
        return mottatteDokumenter.stream()
                .filter(MottattDokument::erSøknadsDokument)
                .max(Comparator.comparing(MottattDokument::getOpprettetTidspunkt)) // Sist mottatte dokument
                .orElse(null);
    }

    private BehandleStegResultat resultatVedIngenMottatteDokument(Behandling behandling, BehandlingLås behandlingLås) {
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.INFOBREV_BEHANDLING)
                || behandling.harBehandlingÅrsak(BehandlingÅrsakType.INFOBREV_OPPHOLD)
                || behandling.harBehandlingÅrsak(BehandlingÅrsakType.INFOBREV_PÅMINNELSE)) {
            var kResultat = KompletthetResultat.ikkeOppfylt(LocalDate.now().plus(VENT_PÅ_SØKNAD_PERIODE).atStartOfDay(),
                    Venteårsak.VENT_SØKNAD_SENDT_INFORMASJONSBREV);
            return evaluerSøknadMottattUoppfylt(behandling, behandlingLås, kResultat, VENT_PÅ_SØKNAD);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean erUstrukturertEndringSøknad(MottattDokument dokument) {
        var dokumentTypeId = dokument.getDokumentType();
        return dokument.getPayloadXml() == null && dokumentTypeId != null && dokumentTypeId.erEndringsSøknadType();
    }

    private boolean erUstrukturertEngangsstønadSøknad(MottattDokument dokument) {
        var dokumentTypeId = dokument.getDokumentType();
        return dokument.getPayloadXml() == null && (DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON.equals(dokumentTypeId)
            || DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL.equals(dokumentTypeId) || DokumentKategori.SØKNAD.equals(dokument.getDokumentKategori()));
    }

    private boolean erUstrukturertForeldrepengerSøknad(MottattDokument dokument) {
        var dokumentTypeId = dokument.getDokumentType();
        return dokument.getPayloadXml() == null && (DokumentTypeId.SØKNAD_FORELDREPENGER_ADOPSJON.equals(dokumentTypeId)
            || DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL.equals(dokumentTypeId) || DokumentKategori.SØKNAD.equals(dokument.getDokumentKategori()));
    }

    private boolean erUstrukturertSvangerskapspengerSøknad(MottattDokument dokument) {
        var dokumentTypeId = dokument.getDokumentType();
        return dokument.getPayloadXml() == null && (DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER.equals(dokumentTypeId) || DokumentKategori.SØKNAD.equals(
            dokument.getDokumentKategori()));
    }

    private BehandleStegResultat evaluerSøknadMottattUoppfylt(Behandling behandling, BehandlingLås lås,
                                                              KompletthetResultat kompletthetResultat, AksjonspunktDefinisjon apDef) {
        if (skalHenleggeBehandling(behandling)) {
            return henlegg(behandling, lås);
        }
        // TODO: Bestill brev (Venter på PK-50295)
        var venteårsak = kompletthetResultat.venteårsak() != null ? kompletthetResultat.venteårsak() : Venteårsak.AVV_DOK;

        var aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(apDef, venteårsak, kompletthetResultat.ventefrist());
        return BehandleStegResultat.utførtMedAksjonspunktResultat(aksjonspunktResultat);
    }

    private boolean skalHenleggeBehandling(Behandling behandling) {
        return behandling.getAksjonspunkter().stream().anyMatch(
            a -> a.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD) && a.getFristTid().isBefore(LocalDateTime.now()));
    }

    private BehandleStegResultat henlegg(Behandling behandling, BehandlingLås behandlingLås) {
        henleggBehandlingTjeneste.forberedHenleggelseFraSteg(behandling, behandlingLås, BehandlingResultatType.HENLAGT_SØKNAD_MANGLER, "Ikke mottatt søknad");
        return BehandleStegResultat.henlagtBehandling();
    }
}
