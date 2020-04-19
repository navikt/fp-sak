package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@Dependent
public class InnhentDokumentTjeneste {

    private static Map<String, DokumentGruppe> DOKUMENTTYPE_TIL_GRUPPE = new HashMap<>();
    static {
        // Søknad
        DokumentTypeId.getSøknadTyper().forEach(v -> DOKUMENTTYPE_TIL_GRUPPE.put(v, DokumentGruppe.SØKNAD));

        // Inntektsmelding
        DOKUMENTTYPE_TIL_GRUPPE.put(DokumentTypeId.INNTEKTSMELDING.getKode(), DokumentGruppe.INNTEKTSMELDING);


        // Endringssøknad
        DokumentTypeId.getEndringSøknadTyper().forEach(v -> DOKUMENTTYPE_TIL_GRUPPE.put(v, DokumentGruppe.ENDRINGSSØKNAD));

        // Klage
        DOKUMENTTYPE_TIL_GRUPPE.put(DokumentTypeId.KLAGE_DOKUMENT.getKode(), DokumentGruppe.KLAGE);
        DOKUMENTTYPE_TIL_GRUPPE.put(DokumentTypeId.KLAGE_ETTERSENDELSE.getKode(), DokumentGruppe.KLAGE);
    }

    private static Map<DokumentKategori, DokumentGruppe> DOKUMENTKATEGORI_TIL_GRUPPE = new HashMap<>();
    static {
        DOKUMENTKATEGORI_TIL_GRUPPE.put(DokumentKategori.SØKNAD, DokumentGruppe.SØKNAD);
        DOKUMENTKATEGORI_TIL_GRUPPE.put(DokumentKategori.KLAGE_ELLER_ANKE, DokumentGruppe.KLAGE);
    }

    private Instance<Dokumentmottaker> mottakere;

    private FagsakRepository fagsakRepository;
    private BehandlingRevurderingRepository revurderingRepository;

    private KøKontroller køKontroller;

    @Inject
    public InnhentDokumentTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                   @Any Instance<Dokumentmottaker> mottakere,
                                   BehandlingRevurderingRepository revurderingRepository,
                                   KøKontroller køKontroller) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.mottakere = mottakere;
        this.revurderingRepository = revurderingRepository;
        this.køKontroller = køKontroller;
    }

    public void utfør(MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(mottattDokument.getFagsakId());
        DokumentTypeId dokumentTypeId = mottattDokument.getDokumentType();

        DokumentGruppe dokumentGruppe = brukDokumentKategori(dokumentTypeId, mottattDokument.getDokumentKategori()) ?
            DOKUMENTKATEGORI_TIL_GRUPPE.getOrDefault(mottattDokument.getDokumentKategori(), DokumentGruppe.VEDLEGG) :
            DOKUMENTTYPE_TIL_GRUPPE.getOrDefault(dokumentTypeId.getKode(), DokumentGruppe.VEDLEGG);

        Dokumentmottaker dokumentmottaker = finnMottaker(dokumentGruppe, fagsak.getYtelseType());
        if (skalMottasSomKøet(fagsak)) {
            dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, behandlingÅrsakType);
            return;
        }
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, behandlingÅrsakType);
    }

    public void opprettFraTidligereBehandling(Long behandlingId, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#SXX og #IXX
        Objects.requireNonNull(behandlingId, "behandlingId"); //$NON-NLS-1$
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(mottattDokument.getFagsakId());
        Dokumentmottaker dokumentmottaker;
        if (mottattDokument.erSøknadsDokument()) {
            dokumentmottaker = finnMottaker(DokumentGruppe.SØKNAD, fagsak.getYtelseType());
        } else if (mottattDokument.getDokumentType().erInntektsmelding()) {
            dokumentmottaker = finnMottaker(DokumentGruppe.INNTEKTSMELDING, fagsak.getYtelseType());
        } else {
            throw new IllegalArgumentException("Utvikler-feil: MottattDokument må være av type søknad eller inntektsmelding, var: " + mottattDokument.getDokumentType().getKode());
        }
        dokumentmottaker.opprettFraTidligereAvsluttetBehandling(fagsak, behandlingId, mottattDokument, behandlingÅrsakType, skalMottasSomKøet(fagsak));
    }

    private boolean skalMottasSomKøet(Fagsak fagsak) {
        Optional<Behandling> åpenBehandling = revurderingRepository.finnÅpenYtelsesbehandling(fagsak.getId());
        if (åpenBehandling.isPresent())
            return åpenBehandling.map(b -> b.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)).orElse(false);
        if (revurderingRepository.finnKøetYtelsesbehandling(fagsak.getId()).isPresent())
            return true;
        Optional<Behandling> åpenBehandlingMedforelder = revurderingRepository.finnÅpenBehandlingMedforelder(fagsak);
        return åpenBehandlingMedforelder.isPresent() && !køKontroller.skalSnikeIKø(fagsak, åpenBehandlingMedforelder.get());
    }

    private boolean brukDokumentKategori(DokumentTypeId dokumentTypeId, DokumentKategori dokumentKategori) {
        return DokumentTypeId.UDEFINERT.equals(dokumentTypeId) ||
            (DokumentKategori.SØKNAD.equals(dokumentKategori) && DokumentTypeId.ANNET.equals(dokumentTypeId));
    }

    private Dokumentmottaker finnMottaker(DokumentGruppe dokumentGruppe, FagsakYtelseType fagsakYtelseType) {
        String dokumentgruppeKode = dokumentGruppe.getKode();
        String fagsakYtelseTypeKode = fagsakYtelseType.getKode();
        Instance<Dokumentmottaker> selected = mottakere.select(new DokumentGruppeRef.DokumentGruppeRefLiteral(dokumentgruppeKode));

        if (selected.isAmbiguous()) {
            selected = selected.select(new FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral(fagsakYtelseTypeKode));
        }

        if (selected.isAmbiguous()) {
            throw new IllegalArgumentException("Mer enn en implementasjon funnet for DokumentGruppe=" + dokumentgruppeKode + ", FagsakYtelseType=" + fagsakYtelseTypeKode);
        } else if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for DokumentGruppe=" + dokumentgruppeKode + ", FagsakYtelseType=" + fagsakYtelseTypeKode);
        }
        Dokumentmottaker minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return minInstans;
    }
}
