package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaSteg;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.RyddRegisterData;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.BehandlingÅrsakTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_FAKTA)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
class KontrollerFaktaRevurderingStegImpl implements KontrollerFaktaSteg {
    private static final Logger LOG = LoggerFactory.getLogger(KontrollerFaktaRevurderingStegImpl.class);

    private static final StartpunktType DEFAULT_STARTPUNKT = StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;

    private BehandlingRepository behandlingRepository;
    private KontrollerFaktaTjeneste tjeneste;
    private BehandlingRepositoryProvider repositoryProvider;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private HentOgLagreBeregningsgrunnlagTjeneste hentBeregningsgrunnlagTjeneste;
    private StartpunktTjeneste startpunktTjeneste;
    private BehandlingÅrsakTjeneste behandlingÅrsakTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private NyeTilretteleggingerTjeneste nyeTilretteleggingerTjeneste;
    private OpptjeningRepository opptjeningRepository;

    KontrollerFaktaRevurderingStegImpl() {
        // for CDI proxy
    }

    @Inject
    KontrollerFaktaRevurderingStegImpl(BehandlingRepositoryProvider repositoryProvider,
                                       BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                       HentOgLagreBeregningsgrunnlagTjeneste hentBeregningsgrunnlagTjeneste,
                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                       @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) KontrollerFaktaTjeneste tjeneste,
                                       @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) StartpunktTjeneste startpunktTjeneste,
                                       BehandlingÅrsakTjeneste behandlingÅrsakTjeneste,
                                       SvangerskapspengerRepository svangerskapspengerRepository,
                                       NyeTilretteleggingerTjeneste nyeTilretteleggingerTjeneste,
                                       MottatteDokumentTjeneste mottatteDokumentTjeneste) {
        this.repositoryProvider = repositoryProvider;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.tjeneste = tjeneste;
        this.hentBeregningsgrunnlagTjeneste = hentBeregningsgrunnlagTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.startpunktTjeneste = startpunktTjeneste;
        this.behandlingÅrsakTjeneste = behandlingÅrsakTjeneste;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.nyeTilretteleggingerTjeneste = nyeTilretteleggingerTjeneste;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.opptjeningRepository = repositoryProvider.getOpptjeningRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        // Spesialhåndtering for enkelte behandlinger
        behandlingÅrsakTjeneste.lagHistorikkForRegisterEndringerMotOriginalBehandling(behandling);

        var startpunkt = utledStartpunkt(ref, behandling);
        behandling.setStartpunkt(startpunkt);

        List<AksjonspunktResultat> aksjonspunktResultater = startpunkt.getRangering() <= StartpunktType.OPPTJENING.getRangering() ?
            tjeneste.utledAksjonspunkterTilHøyreForStartpunkt(ref, startpunkt) : List.of();
        kopierResultaterAvhengigAvStartpunkt(behandling, kontekst);

        if (DEFAULT_STARTPUNKT.equals(startpunkt)) {
            nyeTilretteleggingerTjeneste.utledNyeTilretteleggingerLagreJustert(behandling, skjæringstidspunkter);
        }
        var transisjon = TransisjonIdentifikator.forId(FellesTransisjoner.SPOLFREM_PREFIX + startpunkt.getBehandlingSteg().getKode());
        return BehandleStegResultat.fremoverførtMedAksjonspunktResultater(transisjon, aksjonspunktResultater);
    }


    private StartpunktType utledStartpunkt(BehandlingReferanse ref, Behandling revurdering) {
        var startpunkt = initieltStartPunkt(ref, revurdering);

        // Undersøk behov for GRegulering. Med mindre vi allerede skal til BEREGNING eller tidligere steg
        if (startpunkt.getRangering() > StartpunktType.BEREGNING.getRangering()) {
            var greguleringStartpunkt = utledBehovForGRegulering(ref, revurdering);
            startpunkt = startpunkt.getRangering() < greguleringStartpunkt.getRangering() ? startpunkt : greguleringStartpunkt;
        }

        // Startpunkt for revurdering kan kun hoppe fremover; default dersom startpunkt passert
        if (startpunkt.getRangering() < DEFAULT_STARTPUNKT.getRangering()) {
            startpunkt = DEFAULT_STARTPUNKT;
        }
        LOG.info("KOFAKREV Revurdering {} har fått fastsatt startpunkt {} ", revurdering.getId(), startpunkt.getKode());
        return startpunkt;
    }

    private StartpunktType initieltStartPunkt(BehandlingReferanse ref, Behandling revurdering) {
        var kreverManuell = revurdering.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .anyMatch(ba -> BehandlingÅrsakType.årsakerRelatertTilDød().contains(ba) || BehandlingÅrsakType.årsakerForEtterkontroll().contains(ba))
            || finnesOpplysningerOmDød(ref) || finnesOpplysningerOmDødFødsel(ref);
        var alleTilretteleggingerKopiert = svangerskapspengerRepository.hentGrunnlag(revurdering.getId())
            .map(SvpGrunnlagEntitet::getOpprinneligeTilrettelegginger)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe).orElse(List.of()).stream()
            .allMatch(SvpTilretteleggingEntitet::getKopiertFraTidligereBehandling);
        if (revurdering.erManueltOpprettet() || erEndringssøknad(revurdering) || kreverManuell || !alleTilretteleggingerKopiert) {
            return DEFAULT_STARTPUNKT;
        }
        var orgBehandlingsresultat = getBehandlingsresultat(ref.getOriginalBehandlingId().orElseThrow());
        if (orgBehandlingsresultat == null || orgBehandlingsresultat.isVilkårAvslått()) {
            return DEFAULT_STARTPUNKT;
        }
        var startpunkt = startpunktTjeneste.utledStartpunktMotOriginalBehandling(ref);
        if (StartpunktType.UDEFINERT.equals(startpunkt)) {
            return StartpunktType.UTTAKSVILKÅR;
        }
        return startpunkt;
    }

    private StartpunktType utledBehovForGRegulering(BehandlingReferanse ref, Behandling revurdering) {
        var opprinneligBehandlingId = revurdering.getOriginalBehandlingId()
                .orElseThrow(() -> new IllegalStateException("Revurdering skal ha en basisbehandling - skal ikke skje"));
        var forrigeBeregning = hentBeregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(opprinneligBehandlingId);

        if (forrigeBeregning.isEmpty() || revurdering.harBehandlingÅrsak(BehandlingÅrsakType.RE_SATS_REGULERING)) {
            return StartpunktType.BEREGNING;
        }

        var grunnbeløp = beregningsgrunnlagKopierOgLagreTjeneste.finnEksaktSats(BeregningSatsType.GRUNNBELØP, ref.getSkjæringstidspunkt().getFørsteUttaksdatoGrunnbeløp());
        long satsIBeregning = forrigeBeregning.map(BeregningsgrunnlagEntitet::getGrunnbeløp).map(Beløp::getVerdi).map(BigDecimal::longValue).orElse(0L);

        if (grunnbeløp.getVerdi() - satsIBeregning > 1) {
            return StartpunktType.BEREGNING;
        }
        return StartpunktType.UDEFINERT;
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        var rydder = new RyddRegisterData(repositoryProvider, kontekst);
        rydder.ryddRegisterdataStartpunktRevurdering();
    }

    private void kopierResultaterAvhengigAvStartpunkt(Behandling revurdering,
                                                      BehandlingskontrollKontekst kontekst) {
        var origBehandling = revurdering.getOriginalBehandlingId().map(behandlingRepository::hentBehandling)
                .orElseThrow(() -> new IllegalStateException("Original behandling mangler på revurdering - skal ikke skje"));

        revurdering = kopierVilkårFørStartpunkt(origBehandling, revurdering, kontekst);
        // Skal være kopiert ved opprettelse av revurdering for å få tak i riktig STP.
        // Kan ha blitt nullstilt i denne revurderingen ved tilbakehopp til KOARB (fx pga IM).
        kopierOpptjeningVedBehov(origBehandling, revurdering);

        if (StartpunktType.UTTAKSVILKÅR.equals(revurdering.getStartpunkt()) || StartpunktType.TILKJENT_YTELSE.equals(revurdering.getStartpunkt())) {
            beregningsgrunnlagKopierOgLagreTjeneste.kopierBeregningsresultatFraOriginalBehandling(origBehandling.getId(), revurdering.getId());
        }

    }

    private boolean erEndringssøknad(Behandling revurdering) {
        return revurdering.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                || mottatteDokumentTjeneste.harMottattDokumentSet(revurdering.getId(), Set.of(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER));
    }

    private Behandling kopierVilkårFørStartpunkt(Behandling origBehandling, Behandling revurdering, BehandlingskontrollKontekst kontekst) {
        var vilkårResultat = Optional.ofNullable(getBehandlingsresultat(revurdering.getId()))
                .map(Behandlingsresultat::getVilkårResultat)
                .orElseThrow(() -> new IllegalStateException("VilkårResultat skal alltid være opprettet ved revurdering"));
        var vilkårBuilder = VilkårResultat.builderFraEksisterende(vilkårResultat);

        var startpunkt = revurdering.getStartpunkt();
        var vilkårtyperFørStartpunkt = StartpunktType.finnVilkårHåndtertInnenStartpunkt(startpunkt);
        Objects.requireNonNull(vilkårtyperFørStartpunkt, "Startpunkt " + startpunkt.getKode() +
                " støttes ikke for kopiering av vilkår ved revurdering");

        var originaltBehandlingsresultat = Optional.ofNullable(getBehandlingsresultat(origBehandling.getId())).orElseThrow();
        var vilkårFørStartpunkt = originaltBehandlingsresultat.getVilkårResultat().getVilkårene().stream()
                .filter(vilkår -> vilkårtyperFørStartpunkt.contains(vilkår.getVilkårType()))
                .collect(Collectors.toSet());
        kopierVilkårFørStartpunkt(vilkårBuilder, vilkårFørStartpunkt);
        vilkårBuilder.buildFor(revurdering);

        var revurderingBehandlingsresultat = Optional.ofNullable(getBehandlingsresultat(revurdering.getId())).orElseThrow();
        behandlingRepository.lagre(revurderingBehandlingsresultat.getVilkårResultat(), kontekst.getSkriveLås());
        behandlingRepository.lagre(revurdering, kontekst.getSkriveLås());
        return behandlingRepository.hentBehandling(revurdering.getId());
    }

    private void kopierOpptjeningVedBehov(Behandling origBehandling, Behandling revurdering) {
        if (opptjeningRepository.finnOpptjening(origBehandling.getId()).isPresent() && opptjeningRepository.finnOpptjening(revurdering.getId()).isEmpty()) {
            opptjeningRepository.kopierGrunnlagFraEksisterendeBehandling(origBehandling, revurdering);
        }
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }

    private void kopierVilkårFørStartpunkt(VilkårResultat.Builder vilkårBuilder, Set<Vilkår> vilkårne) {
        vilkårne.forEach(vilkår -> vilkårBuilder.kopierVilkårFraAnnenBehandling(vilkår, false, false));
    }

    private boolean finnesOpplysningerOmDød(BehandlingReferanse ref) {
        return personopplysningRepository.hentPersonopplysningerHvisEksisterer(ref.behandlingId())
            .flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon)
            .map(PersonInformasjonEntitet::getPersonopplysninger).orElse(List.of()).stream()
            .anyMatch(poe -> poe.getDødsdato() != null);
    }

    private boolean finnesOpplysningerOmDødFødsel(BehandlingReferanse ref) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(ref.behandlingId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .filter(fh -> FamilieHendelseType.FØDSEL.equals(fh.getType()))
            .map(FamilieHendelseEntitet::getBarna).orElse(List.of()).stream()
            .anyMatch(poe -> poe.getDødsdato().isPresent());
    }
}
