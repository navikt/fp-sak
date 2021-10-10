package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktUtil.setToTrinnsBehandlingKreves;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktKode;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverhoppResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat.Builder;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ApplicationScoped
public class AksjonspunktTjeneste {

    private static final Set<AksjonspunktDefinisjon> VEDTAK_AP = Set.of(
        AksjonspunktDefinisjon.FORESLÅ_VEDTAK,
        AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL,
        AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT,
        AksjonspunktDefinisjon.FATTER_VEDTAK
    );

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public AksjonspunktTjeneste() {
        // CDI proxy
    }

    @Inject
    public AksjonspunktTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                HenleggBehandlingTjeneste henleggBehandlingTjeneste) {

        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;
    }

    public void bekreftAksjonspunkter(Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        setAnsvarligSaksbehandler(bekreftedeAksjonspunktDtoer, behandling);

        spoolTilbakeTilTidligsteAksjonspunkt(bekreftedeAksjonspunktDtoer, kontekst);

        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);

        var overhoppResultat = bekreftAksjonspunkter(kontekst, bekreftedeAksjonspunktDtoer, behandling, skjæringstidspunkter);

        historikkTjenesteAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.FAKTA_ENDRET);

        behandlingRepository.lagre(getBehandlingsresultat(behandling.getId()).getVilkårResultat(), kontekst.getSkriveLås());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());

        håndterOverhopp(overhoppResultat, kontekst);

        if (behandling.isBehandlingPåVent()) {
            // Skal ikke fortsette behandling dersom behandling ble satt på vent
            return;
        }
        fortsettBehandlingen(behandling, overhoppResultat);// skal ikke reinnhente her, avgjøres i steg?
    }

    protected void setAnsvarligSaksbehandler(Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer, Behandling behandling) {
        if (bekreftedeAksjonspunktDtoer.stream().anyMatch(dto -> dto instanceof FatterVedtakAksjonspunktDto)) {
            return;
        }
        behandling.setAnsvarligSaksbehandler(getCurrentUserId());
    }

    protected String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

    private void spoolTilbakeTilTidligsteAksjonspunkt(Collection<? extends AksjonspunktKode> aksjonspunktDtoer,
                                                      BehandlingskontrollKontekst kontekst) {
        // NB: Første løsning på tilbakeføring ved endring i GUI (når aksjonspunkter tilhørende eldre enn aktivt steg
        // sendes inn spoles prosessen
        // tilbake). Vil utvides etter behov når regler for spoling bakover blir klarere.
        // Her sikres at behandlingskontroll hopper tilbake til aksjonspunktenes tidligste "løsesteg" dersom aktivt
        // behandlingssteg er lenger fremme i sekvensen
        var bekreftedeApKoder = aksjonspunktDtoer.stream()
            .map(AksjonspunktKode::getAksjonspunktDefinisjon)
            .collect(toList());

        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligsteAksjonspunkt(kontekst, bekreftedeApKoder);
    }

    private void håndterOverhopp(OverhoppResultat overhoppResultat, BehandlingskontrollKontekst kontekst) {
        // TODO (essv): PKMANTIS-1992 Skrive om alle overhopp til å bruke transisjon (se fremoverTransisjon nedenfor)
        var funnetHenleggelse = overhoppResultat.finnHenleggelse();
        if (funnetHenleggelse.isPresent()) {
            var henleggelse = funnetHenleggelse.get();
            henleggBehandlingTjeneste.henleggBehandling(kontekst.getBehandlingId(),
                henleggelse.getHenleggelseResultat(), henleggelse.getHenleggingsbegrunnelse());
        } else {
            var fremoverTransisjon = overhoppResultat.finnFremoverTransisjon();
            if (fremoverTransisjon.isPresent()) {
                var riktigTransisjon = utledFremhoppTransisjon(kontekst, fremoverTransisjon.get());
                behandlingskontrollTjeneste.fremoverTransisjon(riktigTransisjon, kontekst);
            }
        }
    }

    public void overstyrAksjonspunkter(Collection<OverstyringAksjonspunktDto> overstyrteAksjonspunkter, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);

        // Tilbakestill gjeldende steg før fremføring
        spoolTilbakeTilTidligsteAksjonspunkt(overstyrteAksjonspunkter, kontekst);

        var overhoppForOverstyring = overstyrVilkårEllerBeregning(overstyrteAksjonspunkter, behandling, kontekst);

        var utførteAksjonspunkter = lagreHistorikkInnslag(behandling);

        behandlingskontrollTjeneste.aksjonspunkterEndretStatus(kontekst, behandling.getAktivtBehandlingSteg(), utførteAksjonspunkter);

        // Fremoverhopp hvis vilkår settes til AVSLÅTT
        håndterOverhopp(overhoppForOverstyring, kontekst);

        if (behandling.isBehandlingPåVent()) {
            // Skal ikke fortsette behandling dersom behandling ble satt på vent
            return;
        }
        fortsettBehandlingen(behandling, overhoppForOverstyring);// skal ikke reinnhente her, avgjøres i steg?
    }

    private void fortsettBehandlingen(Behandling behandling, OverhoppResultat overhoppResultat) {
        if (overhoppResultat.skalOppdatereGrunnlag()) {
            behandlingsprosessTjeneste.asynkRegisteroppdateringKjørProsess(behandling);
        } else {
            behandlingsprosessTjeneste.asynkKjørProsess(behandling);
        }
    }

    private boolean harVilkårResultat(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).map(Behandlingsresultat::getVilkårResultat).isPresent();
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hent(behandlingId);
    }

    private TransisjonIdentifikator utledFremhoppTransisjon(BehandlingskontrollKontekst kontekst, TransisjonIdentifikator transisjon) {
        if (FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR.equals(transisjon)) {
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            if (behandling.erRevurdering() && FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsak().getYtelseType())
                && !harAvslåttForrigeBehandling(behandling)) {
                return FellesTransisjoner.FREMHOPP_TIL_UTTAKSPLAN;
            }
            return FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT;
        }
        return transisjon;
    }

    private boolean harAvslåttForrigeBehandling(Behandling revurdering) {
        var originalBehandlingId = revurdering.getOriginalBehandlingId();
        if (originalBehandlingId.isPresent()) {
            var behandlingsresultat = getBehandlingsresultat(originalBehandlingId.get());
            // Dersom originalBehandling er et beslutningsvedtak må vi lete videre etter det faktiske resultatet for å kunne vurdere om forrige
            // behandling var avslått
            if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingsresultat.getBehandlingResultatType())) {
                return harAvslåttForrigeBehandling(behandlingRepository.hentBehandling(originalBehandlingId.get()));
            }
            return behandlingsresultat.isBehandlingsresultatAvslått();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private OverhoppResultat overstyrVilkårEllerBeregning(Collection<OverstyringAksjonspunktDto> overstyrteAksjonspunkter,
                                                          Behandling behandling, BehandlingskontrollKontekst kontekst) {
        var overhoppResultat = OverhoppResultat.tomtResultat();

        // oppdater for overstyring
        overstyrteAksjonspunkter.forEach(dto -> {
            @SuppressWarnings("rawtypes")
            Overstyringshåndterer overstyringshåndterer = finnOverstyringshåndterer(dto);
            var oppdateringResultat = overstyringshåndterer.håndterOverstyring(dto, behandling, kontekst);
            overhoppResultat.leggTil(oppdateringResultat);

            settToTrinnPåOverstyrtAksjonspunktHvisKreves(behandling, dto, oppdateringResultat.kreverTotrinnsKontroll());
        });

        // legg til overstyring aksjonspunkt (normalt vil være utført) og historikk
        overstyrteAksjonspunkter.forEach(dto -> {
            @SuppressWarnings("rawtypes")
            Overstyringshåndterer overstyringshåndterer = finnOverstyringshåndterer(dto);
            overstyringshåndterer.håndterAksjonspunktForOverstyringPrecondition(dto, behandling);
            var aksjonspunktDefinisjon = overstyringshåndterer.aksjonspunktForInstans();
            opprettAksjonspunktForOverstyring(kontekst, behandling, dto, aksjonspunktDefinisjon);
            overstyringshåndterer.håndterAksjonspunktForOverstyringHistorikk(dto, behandling);
        });

        var totrinn = overhoppResultat.finnTotrinn();
        overhoppResultat.finnEkstraAksjonspunktResultat().forEach(res -> håndterEkstraAksjonspunktResultat(kontekst, behandling, totrinn, res.aksjonspunktResultat(), res.aksjonspunktStatus(), true));

        return overhoppResultat;
    }

    private void opprettAksjonspunktForOverstyring(BehandlingskontrollKontekst kontekst, Behandling behandling, OverstyringAksjonspunkt dto, AksjonspunktDefinisjon apDef) {
        var eksisterendeAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(apDef);
        var aksjonspunkt = eksisterendeAksjonspunkt.orElseGet(() -> behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(apDef)).get(0));

        if (aksjonspunkt.erAvbrutt()) {
            // Må reåpne avbrutte før de kan settes til utført (kunne ha vært én operasjon i aksjonspunktRepository)
            behandlingskontrollTjeneste.lagreAksjonspunkterReåpnet(kontekst, List.of(aksjonspunkt), true, false);
            behandlingskontrollTjeneste.lagreAksjonspunkterUtført(kontekst, null, aksjonspunkt, dto.getBegrunnelse());
        } else {
            behandlingskontrollTjeneste.lagreAksjonspunkterUtført(kontekst, null, aksjonspunkt, dto.getBegrunnelse());
        }
    }

    private void håndterEkstraAksjonspunktResultat(BehandlingskontrollKontekst kontekst, Behandling behandling, boolean totrinn, AksjonspunktResultat apRes, AksjonspunktStatus nyStatus, boolean overstyring) {
        var eksisterendeAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(apRes.getAksjonspunktDefinisjon());
        var aksjonspunkt = eksisterendeAksjonspunkt.orElseGet(() -> opprettEkstraAksjonspunktForResultat(kontekst, behandling, apRes, overstyring));

        if (totrinn && !AksjonspunktStatus.AVBRUTT.equals(nyStatus)  && aksjonspunktStøtterTotrinn(aksjonspunkt)) {
            setToTrinnsBehandlingKreves(aksjonspunkt);
        }
        if (nyStatus.equals(aksjonspunkt.getStatus())) {
            return;
        }
        if (AksjonspunktStatus.OPPRETTET.equals(nyStatus)) {
            behandlingskontrollTjeneste.lagreAksjonspunkterReåpnet(kontekst, List.of(aksjonspunkt), true, false);
        } else if (AksjonspunktStatus.AVBRUTT.equals(nyStatus)) {
            behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), List.of(aksjonspunkt));
        } else {
            if (aksjonspunkt.erAvbrutt()) {
                // Må reåpne avbrutte før de kan settes til utført (kunne ha vært én operasjon i aksjonspunktRepository)
                behandlingskontrollTjeneste.lagreAksjonspunkterReåpnet(kontekst, List.of(aksjonspunkt), true, false);
            }
            behandlingskontrollTjeneste.lagreAksjonspunkterUtført(kontekst, null, List.of(aksjonspunkt));
        }
    }

    private Aksjonspunkt opprettEkstraAksjonspunktForResultat(BehandlingskontrollKontekst kontekst, Behandling behandling, AksjonspunktResultat apRes, boolean overstyring) {
        if (apRes.getAksjonspunktDefinisjon().erAutopunkt() && apRes.getFrist() != null) {
            return behandlingskontrollTjeneste.settBehandlingPåVent(behandling, apRes.getAksjonspunktDefinisjon(), behandling.getAktivtBehandlingSteg(), apRes.getFrist(), apRes.getVenteårsak());
        }
        if (overstyring) {
            return behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(apRes.getAksjonspunktDefinisjon())).get(0);
        }
        return behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, behandling.getAktivtBehandlingSteg(), List.of(apRes.getAksjonspunktDefinisjon())).get(0);
    }


    private List<Aksjonspunkt> lagreHistorikkInnslag(Behandling behandling) {

        // TODO(FC): Kan vi flytte spesielhåndtering av SØKERS_OPPLYSNINGSPLIKT_OVST ned til
        // SøkersOpplysningspliktOverstyringshåndterer?
        // vis vi aldri sender inn mer enn en overstyring kan historikk opprettes også der.

        var utførteAksjonspunkter = behandling
            .getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST)
            .map(Collections::singletonList)
            .orElse(emptyList());
        if (utførteAksjonspunkter.isEmpty()) {
            historikkTjenesteAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.OVERSTYRT);
        } else {
            // SØKERS_OPPLYSNINGSPLIKT_OVST skal gi et "vanlig" historikkinnslag
            historikkTjenesteAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.FAKTA_ENDRET);
        }

        return utførteAksjonspunkter;
    }

    private OverhoppResultat bekreftAksjonspunkter(BehandlingskontrollKontekst kontekst,
                                                   Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer,
                                                   Behandling behandling,
                                                   Skjæringstidspunkt skjæringstidspunkter) {

        var overhoppResultat = OverhoppResultat.tomtResultat();

        var vilkårBuilder = harVilkårResultat(behandling)
            ? VilkårResultat.builderFraEksisterende(getBehandlingsresultat(behandling.getId()).getVilkårResultat())
            : VilkårResultat.builder();

        bekreftedeAksjonspunktDtoer
            .forEach(dto -> bekreftAksjonspunkt(kontekst, behandling, skjæringstidspunkter, vilkårBuilder, overhoppResultat, dto));

        var vilkårResultat = vilkårBuilder.buildFor(behandling);
        behandlingRepository.lagre(vilkårResultat, kontekst.getSkriveLås());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());

        var totrinn = overhoppResultat.finnTotrinn();
        overhoppResultat.finnEkstraAksjonspunktResultat().forEach(res -> håndterEkstraAksjonspunktResultat(kontekst, behandling, totrinn, res.aksjonspunktResultat(), res.aksjonspunktStatus(), false));

        return overhoppResultat;
    }

    private void bekreftAksjonspunkt(BehandlingskontrollKontekst kontekst,
                                     Behandling behandling,
                                     Skjæringstidspunkt skjæringstidspunkter,
                                     Builder vilkårBuilder,
                                     OverhoppResultat overhoppResultat,
                                     BekreftetAksjonspunktDto dto) {
        // Endringskontroll for aksjonspunkt
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        var oppdaterer = finnAksjonspunktOppdaterer(dto.getClass(), dto.getAksjonspunktDefinisjon());
        var param = new AksjonspunktOppdaterParameter(behandling, Optional.of(aksjonspunkt), skjæringstidspunkter, dto);
        var delresultat = oppdaterer.oppdater(dto, param);
        overhoppResultat.leggTil(delresultat);
        byggVilkårResultat(vilkårBuilder, delresultat);

        if (delresultat.kreverTotrinnsKontroll()) {
            setToTrinnsBehandlingKreves(aksjonspunkt);
        }

        if (!aksjonspunkt.erAvbrutt() && delresultat.skalUtføreAksjonspunkt()) {
            behandlingskontrollTjeneste.lagreAksjonspunkterUtført(kontekst, behandling.getAktivtBehandlingSteg(), aksjonspunkt, dto.getBegrunnelse());
        }
    }

    private void byggVilkårResultat(Builder vilkårResultatBuilder, OppdateringResultat delresultat) {
        delresultat.getVilkårTyperNyeIkkeVurdert()
            .forEach(vilkårResultatBuilder::leggTilVilkårIkkeVurdert);
        delresultat.getVilkårUtfallSomSkalLeggesTil()
            .forEach(v -> vilkårResultatBuilder.manueltVilkår(v.getVilkårType(), v.getVilkårUtfallType(), v.getAvslagsårsak()));
        delresultat.getVilkårTyperSomSkalFjernes().forEach(vilkårResultatBuilder::fjernVilkår); // TODO: Vilkår burde ryddes på ein annen måte enn dette
        if (delresultat.getVilkårResultatType() != null) {
            vilkårResultatBuilder.medVilkårResultatType(delresultat.getVilkårResultatType());
        }
    }

    @SuppressWarnings("unchecked")
    private AksjonspunktOppdaterer<BekreftetAksjonspunktDto> finnAksjonspunktOppdaterer(Class<? extends BekreftetAksjonspunktDto> dtoClass,
                                                                                        AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var instance = finnAdapter(dtoClass, AksjonspunktOppdaterer.class);
        if (instance.isUnsatisfied()) {
            throw new TekniskException("FP-770743",
                "Finner ikke håndtering for aksjonspunkt med kode: " + aksjonspunktDefinisjon.getKode());
        }
        var minInstans = instance.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException(
                "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return (AksjonspunktOppdaterer<BekreftetAksjonspunktDto>) minInstans;

    }

    private Instance<Object> finnAdapter(Class<?> cls, final Class<?> targetAdapter) {
        var cdi = CDI.current();
        var instance = cdi.select(new DtoTilServiceAdapter.Literal(cls, targetAdapter));

        // hvis unsatisfied, søk parent
        while (instance.isUnsatisfied() && !Objects.equals(Object.class, cls)) {
            cls = cls.getSuperclass();
            instance = cdi.select(new DtoTilServiceAdapter.Literal(cls, targetAdapter));
            if (!instance.isUnsatisfied()) {
                return instance;
            }
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    private <V extends OverstyringAksjonspunktDto> Overstyringshåndterer<V> finnOverstyringshåndterer(V dto) {
        var instance = finnAdapter(dto.getClass(), Overstyringshåndterer.class);

        if (instance.isUnsatisfied()) {
            throw new TekniskException("FP-475766",
                "Finner ikke overstyringshåndterer for DTO: " + dto.getClass().getSimpleName());
        }
        var minInstans = instance.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException(
                "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return (Overstyringshåndterer<V>) minInstans;
    }

    private void settToTrinnPåOverstyrtAksjonspunktHvisKreves(Behandling behandling, OverstyringAksjonspunktDto dto,
                                                              boolean resultatKreverTotrinn) {
        var aksjonspunktDefinisjon = dto.getAksjonspunktDefinisjon();
        if (resultatKreverTotrinn && behandling.harAksjonspunktMedType(aksjonspunktDefinisjon)) {
            var aksjonspunkt = behandling.getAksjonspunktFor(aksjonspunktDefinisjon);
            setToTrinnsBehandlingKreves(aksjonspunkt);
        }
    }

    private boolean aksjonspunktStøtterTotrinn(Aksjonspunkt aksjonspunkt) {
        var aksjonspunktSkjermlenkeType = aksjonspunkt.getAksjonspunktDefinisjon().getSkjermlenkeType();
        return !VEDTAK_AP.contains(aksjonspunkt.getAksjonspunktDefinisjon())
            // Aksjonspunkter må ha SkjermlenkeType for å støtte totrinnskontroll
            && aksjonspunktSkjermlenkeType != null
            && !SkjermlenkeType.UDEFINERT.equals(aksjonspunktSkjermlenkeType);
    }
}
