package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktKode;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdateringTransisjon;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringAksjonspunktResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverhoppResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktkontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingSaksbehandlerEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat.Builder;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class AksjonspunktTjeneste {

    private static final Set<AksjonspunktDefinisjon> VEDTAK_AP_UTEN_TOTRINN = Set.of(
        AksjonspunktDefinisjon.FATTER_VEDTAK
    );

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;

    private BehandlingEventPubliserer behandlingEventPubliserer;

    private AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste;

    public AksjonspunktTjeneste() {
        // CDI proxy
    }

    @Inject
    public AksjonspunktTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                                BehandlingEventPubliserer behandlingEventPubliserer,
                                AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste) {

        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingEventPubliserer = behandlingEventPubliserer;
        this.aksjonspunktkontrollTjeneste = aksjonspunktkontrollTjeneste;
    }

    void bekreftAksjonspunkter(Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer, Behandling behandling, BehandlingLås skriveLås) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, skriveLås);
        setAnsvarligSaksbehandler(bekreftedeAksjonspunktDtoer, behandling);

        spoolTilbakeTilTidligsteAksjonspunkt(bekreftedeAksjonspunktDtoer, kontekst);

        var overhoppResultat = bekreftAksjonspunkter(behandling, skriveLås, bekreftedeAksjonspunktDtoer);

        behandlingRepository.lagre(getBehandlingsresultat(behandling.getId()).getVilkårResultat(), skriveLås);
        behandlingRepository.lagre(behandling, skriveLås);

        håndterOverhopp(behandling, overhoppResultat, kontekst);

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
        var aktivBruker = getCurrentUserId();
        if (!Objects.equals(behandling.getAnsvarligSaksbehandler(), aktivBruker)) {
            behandling.setAnsvarligSaksbehandler(aktivBruker);
            // Datavarehus
            behandlingEventPubliserer.publiserBehandlingEvent(new BehandlingSaksbehandlerEvent(behandling));
        }
    }

    protected String getCurrentUserId() {
        return KontekstHolder.getKontekst().getUid();
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
            .toList();

        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligsteAksjonspunkt(kontekst, bekreftedeApKoder);
    }

    private void håndterOverhopp(Behandling behandling, OverhoppResultat overhoppResultat, BehandlingskontrollKontekst kontekst) {
        overhoppResultat.finnFremoverTransisjon().ifPresent(framoverTransisjon -> {
            var riktigSteg = utledFremhoppSteg(behandling, framoverTransisjon);
            behandlingskontrollTjeneste.fremoverTransisjon(kontekst, riktigSteg);
        });
    }

    public void overstyrAksjonspunkter(Collection<OverstyringAksjonspunktDto> overstyrteAksjonspunkter, Behandling behandling, BehandlingLås skriveLås) {
        if (SpesialBehandling.kanIkkeOverstyres(behandling)) {
            throw new FunksjonellException("FP-760744", "Behandlingen kan ikke overstyres og må gjennomføres",
                "Vurder behov for ordinær revurdering etter at denne behnadlingen er avsluttet");
        }
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, skriveLås);
        setAnsvarligSaksbehandler(List.of(), behandling);

        // Tilbakestill gjeldende steg før fremføring
        spoolTilbakeTilTidligsteAksjonspunkt(overstyrteAksjonspunkter, kontekst);

        var overhoppForOverstyring = overstyrVilkårEllerBeregning(overstyrteAksjonspunkter, behandling, skriveLås);

        // Fremoverhopp hvis vilkår settes til AVSLÅTT
        håndterOverhopp(behandling, overhoppForOverstyring, kontekst);

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

    private BehandlingStegType utledFremhoppSteg(Behandling behandling, AksjonspunktOppdateringTransisjon transisjon) {
        if (AksjonspunktOppdateringTransisjon.AVSLAG_VILKÅR.equals(transisjon)) {
            if (behandling.erRevurdering() && !FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsak().getYtelseType())
                && !harAvslåttForrigeBehandling(behandling)) {
                return BehandlingStegType.INNGANG_UTTAK;
            }
            return BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT;
        } else if (AksjonspunktOppdateringTransisjon.FORESLÅ_VEDTAK.equals(transisjon)) {
            return BehandlingStegType.FORESLÅ_VEDTAK;
        } else {
            throw new IllegalArgumentException("Utviklerfeil: mangler mapping av aksjonspunkttransisjon til transisasjon");
        }
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
                                                          Behandling behandling, BehandlingLås skriveLås) {
        var overhoppResultat = OverhoppResultat.tomtResultat();
        var ref = BehandlingReferanse.fra(behandling);

        // oppdater for overstyring
        overstyrteAksjonspunkter.forEach(dto -> {
            @SuppressWarnings("rawtypes")
            Overstyringshåndterer overstyringshåndterer = finnOverstyringshåndterer(dto);
            var oppdateringResultat = overstyringshåndterer.håndterOverstyring(dto, ref);
            overhoppResultat.leggTil(oppdateringResultat);

            settToTrinnPåOverstyrtAksjonspunktHvisKreves(behandling, skriveLås, dto, oppdateringResultat.kreverTotrinnsKontroll());
        });

        // legg til overstyring aksjonspunkt (normalt vil være utført) og historikk
        overstyrteAksjonspunkter.forEach(dto -> {
            @SuppressWarnings("rawtypes")
            Overstyringshåndterer overstyringshåndterer = finnOverstyringshåndterer(dto);
            overstyringshåndterer.precondition(dto, ref);
            var aksjonspunktDefinisjon = dto.getAksjonspunktDefinisjon();
            opprettAksjonspunktForOverstyring(behandling, skriveLås, dto, aksjonspunktDefinisjon);
            overstyringshåndterer.lagHistorikkInnslag(dto, ref);
        });

        var totrinn = overhoppResultat.finnTotrinn();
        overhoppResultat.finnEkstraAksjonspunktResultat()
            .forEach(res -> håndterEkstraAksjonspunktResultat(behandling, skriveLås, totrinn, res, true));

        return overhoppResultat;
    }

    private void opprettAksjonspunktForOverstyring(Behandling behandling, BehandlingLås skriveLås, OverstyringAksjonspunkt dto, AksjonspunktDefinisjon apDef) {
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(apDef)
            .orElseGet(() -> aksjonspunktkontrollTjeneste.lagreAksjonspunkterFunnet(behandling, skriveLås, List.of(apDef)).getFirst());

        if (aksjonspunkt.erAvbrutt()) {
            // Må reåpne avbrutte før de kan settes til utført (kunne ha vært én operasjon i aksjonspunktRepository)
            aksjonspunktkontrollTjeneste.lagreAksjonspunkterReåpnet(behandling, skriveLås, List.of(aksjonspunkt));
            aksjonspunktkontrollTjeneste.lagreAksjonspunkterUtført(behandling, skriveLås, aksjonspunkt, dto.getBegrunnelse());
        } else {
            aksjonspunktkontrollTjeneste.lagreAksjonspunkterUtført(behandling, skriveLås, aksjonspunkt, dto.getBegrunnelse());
        }
    }

    private void håndterEkstraAksjonspunktResultat(Behandling behandling, BehandlingLås skriveLås, boolean totrinn,
                                                   OppdateringAksjonspunktResultat apRes, boolean overstyring) {
        var nyStatus = apRes.aksjonspunktStatus();
        var eksisterendeAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(apRes.aksjonspunktDefinisjon());
        var aksjonspunkt = eksisterendeAksjonspunkt.orElseGet(() -> opprettEkstraAksjonspunktForResultat(behandling, skriveLås, apRes, overstyring));

        if (totrinn && !AksjonspunktStatus.AVBRUTT.equals(nyStatus)  && aksjonspunktStøtterTotrinn(aksjonspunkt)) {
            aksjonspunktkontrollTjeneste.setAksjonspunkterToTrinn(behandling, skriveLås, List.of(aksjonspunkt), true);
        }
        if (nyStatus.equals(aksjonspunkt.getStatus())) {
            return;
        }
        if (AksjonspunktStatus.OPPRETTET.equals(nyStatus)) {
            aksjonspunktkontrollTjeneste.lagreAksjonspunkterReåpnet(behandling, skriveLås, List.of(aksjonspunkt));
        } else if (AksjonspunktStatus.AVBRUTT.equals(nyStatus)) {
            aksjonspunktkontrollTjeneste.lagreAksjonspunkterAvbrutt(behandling, skriveLås, List.of(aksjonspunkt));
        } else {
            if (aksjonspunkt.erAvbrutt()) {
                // Må reåpne avbrutte før de kan settes til utført (kunne ha vært én operasjon i aksjonspunktRepository)
                aksjonspunktkontrollTjeneste.lagreAksjonspunkterReåpnet(behandling, skriveLås, List.of(aksjonspunkt));
            }
            aksjonspunktkontrollTjeneste.lagreAksjonspunkterUtført(behandling, skriveLås, aksjonspunkt, aksjonspunkt.getBegrunnelse());
        }
    }

    private Aksjonspunkt opprettEkstraAksjonspunktForResultat(Behandling behandling, BehandlingLås skriveLås, OppdateringAksjonspunktResultat apRes, boolean overstyring) {
        if (overstyring) {
            return aksjonspunktkontrollTjeneste.lagreAksjonspunkterFunnet(behandling, skriveLås, List.of(apRes.aksjonspunktDefinisjon())).getFirst();
        } else {
            return aksjonspunktkontrollTjeneste.lagreAksjonspunkterFunnet(behandling, skriveLås, behandling.getAktivtBehandlingSteg(), List.of(apRes.aksjonspunktDefinisjon())).getFirst();
        }
    }

    private OverhoppResultat bekreftAksjonspunkter(Behandling behandling, BehandlingLås skriveLås,
                                                   Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer) {

        var overhoppResultat = OverhoppResultat.tomtResultat();

        var vilkårBuilder = harVilkårResultat(behandling)
            ? VilkårResultat.builderFraEksisterende(getBehandlingsresultat(behandling.getId()).getVilkårResultat())
            : VilkårResultat.builder();

        bekreftedeAksjonspunktDtoer
            .forEach(dto -> bekreftAksjonspunkt(behandling, skriveLås, vilkårBuilder, overhoppResultat, dto));

        var vilkårResultat = vilkårBuilder.buildFor(behandling);
        behandlingRepository.lagre(vilkårResultat, skriveLås);
        behandlingRepository.lagre(behandling, skriveLås);

        var totrinn = overhoppResultat.finnTotrinn();
        overhoppResultat.finnEkstraAksjonspunktResultat()
            .forEach(res -> håndterEkstraAksjonspunktResultat(behandling, skriveLås, totrinn, res, false));

        return overhoppResultat;
    }

    private void bekreftAksjonspunkt(Behandling behandling, BehandlingLås skriveLås, Builder vilkårBuilder,
                                     OverhoppResultat overhoppResultat,
                                     BekreftetAksjonspunktDto dto) {
        // Endringskontroll for aksjonspunkt
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());

        var oppdaterer = finnAksjonspunktOppdaterer(dto.getClass(), dto.getAksjonspunktDefinisjon());
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt);
        var delresultat = oppdaterer.oppdater(dto, param);
        overhoppResultat.leggTil(delresultat);
        byggVilkårResultat(vilkårBuilder, delresultat);

        if (delresultat.kreverTotrinnsKontroll() && aksjonspunktStøtterTotrinn(aksjonspunkt)) {
            aksjonspunktkontrollTjeneste.setAksjonspunkterToTrinn(behandling, skriveLås, List.of(aksjonspunkt), true);
        }

        if (!aksjonspunkt.erAvbrutt() && delresultat.skalUtføreAksjonspunkt()) {
            aksjonspunktkontrollTjeneste.lagreAksjonspunkterUtført(behandling, skriveLås, aksjonspunkt, dto.getBegrunnelse());
        }
    }

    private void byggVilkårResultat(Builder vilkårResultatBuilder, OppdateringResultat delresultat) {
        delresultat.getVilkårTyperNyeIkkeVurdert()
            .forEach(vilkårResultatBuilder::leggTilVilkårIkkeVurdert);
        delresultat.getVilkårUtfallSomSkalLeggesTil()
            .forEach(v -> vilkårResultatBuilder.manueltVilkår(v.getVilkårType(), v.getVilkårUtfallType(), v.getAvslagsårsak()));
        delresultat.getVilkårTyperSomSkalFjernes().forEach(vilkårResultatBuilder::fjernVilkår); // TODO: Vilkår burde ryddes på ein annen måte enn dette
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

    private void settToTrinnPåOverstyrtAksjonspunktHvisKreves(Behandling behandling, BehandlingLås skriveLås,
                                                              OverstyringAksjonspunktDto dto, boolean resultatKreverTotrinn) {
        var aksjonspunktDefinisjon = dto.getAksjonspunktDefinisjon();
        if (resultatKreverTotrinn && behandling.harAksjonspunktMedType(aksjonspunktDefinisjon)) {
            var aksjonspunkt = behandling.getAksjonspunktFor(aksjonspunktDefinisjon);
            aksjonspunktkontrollTjeneste.setAksjonspunkterToTrinn(behandling, skriveLås, List.of(aksjonspunkt), true);
        }
    }

    private boolean aksjonspunktStøtterTotrinn(Aksjonspunkt aksjonspunkt) {
        return !VEDTAK_AP_UTEN_TOTRINN.contains(aksjonspunkt.getAksjonspunktDefinisjon())
            // Aksjonspunkter må ha SkjermlenkeType for å støtte totrinnskontroll
            && aksjonspunkt.kanSetteToTrinnsbehandling();
    }
}
