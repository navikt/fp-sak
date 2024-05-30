package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.domene.uttak.KopierForeldrepengerUttaktjeneste;
import no.nav.foreldrepenger.domene.uttak.SkalKopiereUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.LoggInfoOmArbeidsforholdAktivitetskrav;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettUttakManueltAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@BehandlingStegRef(BehandlingStegType.VURDER_UTTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class UttakStegImpl implements UttakSteg {

    private static final Logger LOG = LoggerFactory.getLogger(UttakStegImpl.class);
    private final FastsettePerioderTjeneste fastsettePerioderTjeneste;
    private final FastsettUttakManueltAksjonspunktUtleder fastsettUttakManueltAksjonspunktUtleder;
    private final FpUttakRepository fpUttakRepository;
    private final UttakInputTjeneste uttakInputTjeneste;
    private final UttakStegBeregnStønadskontoTjeneste beregnStønadskontoTjeneste;
    private final SkalKopiereUttakTjeneste skalKopiereUttakTjeneste;
    private final KopierForeldrepengerUttaktjeneste kopierUttaktjeneste;
    private final LoggInfoOmArbeidsforholdAktivitetskrav loggArbeidsforholdInfo;
    private final YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    public UttakStegImpl(BehandlingRepositoryProvider repositoryProvider,
                         FastsettePerioderTjeneste fastsettePerioderTjeneste,
                         FastsettUttakManueltAksjonspunktUtleder fastsettUttakManueltAksjonspunktUtleder,
                         UttakInputTjeneste uttakInputTjeneste,
                         UttakStegBeregnStønadskontoTjeneste beregnStønadskontoTjeneste,
                         SkalKopiereUttakTjeneste skalKopiereUttakTjeneste, KopierForeldrepengerUttaktjeneste kopierUttaktjeneste,
                         LoggInfoOmArbeidsforholdAktivitetskrav loggArbeidsforholdInfo,
                         YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.fastsettUttakManueltAksjonspunktUtleder = fastsettUttakManueltAksjonspunktUtleder;
        this.fastsettePerioderTjeneste = fastsettePerioderTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.beregnStønadskontoTjeneste = beregnStønadskontoTjeneste;
        this.skalKopiereUttakTjeneste = skalKopiereUttakTjeneste;
        this.kopierUttaktjeneste = kopierUttaktjeneste;
        this.loggArbeidsforholdInfo = loggArbeidsforholdInfo;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();

        var input = uttakInputTjeneste.lagInput(behandlingId);

        try {
            loggInformasjonOmArbeidsforholdVedAktivitetskravFellesperiode(input);
        } catch (Exception e){
            LOG.info("VurderUttakDokumentasjonAksjonspunktUtleder: Feil ved logging av arbeidsforhold når fellesperiode og aktivitetskrav ARBEID på {}", input.getBehandlingReferanse().saksnummer(), e);
        }

        var kontoutregningForBehandling = beregnStønadskontoTjeneste.fastsettStønadskontoerForBehandling(input);

        fastsettePerioderTjeneste.fastsettePerioder(input, kontoutregningForBehandling);

        var aksjonspunkter = fastsettUttakManueltAksjonspunktUtleder.utledAksjonspunkterFor(input)
            .stream().map(AksjonspunktResultat::opprettForAksjonspunkt).toList();
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }

    private void loggInformasjonOmArbeidsforholdVedAktivitetskravFellesperiode(UttakInput input) {
        var behandlingId = input.getBehandlingReferanse().behandlingId();
        var ytelseaggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);
        var aktuellePerioder = ytelseaggregat.getGjeldendeFordeling().getPerioder().stream()
            .filter(p -> MorsAktivitet.ARBEID.equals(p.getMorsAktivitet()) && erAktivitetskravVurdert(p.getDokumentasjonVurdering()))
            .toList();

        if (aktuellePerioder.isEmpty()) {
            return;
        }

        loggArbeidsforholdInfo.loggInfoOmArbeidsforhold(input.getBehandlingReferanse(), ytelseaggregat, aktuellePerioder);
    }

    private boolean erAktivitetskravVurdert(DokumentasjonVurdering dokumentasjonVurdering) {
        return switch (dokumentasjonVurdering) {
            case MORS_AKTIVITET_GODKJENT, MORS_AKTIVITET_IKKE_GODKJENT, MORS_AKTIVITET_IKKE_DOKUMENTERT -> true;
            case null -> false;
            default -> false;
        };
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst,
                                   BehandlingStegModell modell,
                                   BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        if (!Objects.equals(BehandlingStegType.VURDER_UTTAK, førsteSteg)) {
            ryddUttak(kontekst.getBehandlingId());
        }
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst,
                                    BehandlingStegModell modell,
                                    BehandlingStegType førsteSteg,
                                    BehandlingStegType sisteSteg) {
        // TODO(jol) bedre grensesnitt for henleggelser TFP-3721
        if (BehandlingStegType.IVERKSETT_VEDTAK.equals(sisteSteg)) {
            return;
        }
        var uttakInput = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        if (skalKopiereUttakTjeneste.skalKopiereStegResultat(uttakInput)) {
            kopierUttaktjeneste.kopierUttaksresultatFraOriginalBehandling(uttakInput.getBehandlingReferanse().getOriginalBehandlingId().orElseThrow(),
                uttakInput.getBehandlingReferanse().behandlingId());
        } else {
            ryddUttak(kontekst.getBehandlingId());
        }
    }

    private void ryddUttak(Long behandlingId) {
        fpUttakRepository.deaktivterAktivtResultat(behandlingId);
    }

}
