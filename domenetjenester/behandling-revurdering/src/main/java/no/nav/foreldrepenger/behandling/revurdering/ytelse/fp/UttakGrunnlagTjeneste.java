package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.input.*;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

import java.time.Period;
import java.util.Optional;

@ApplicationScoped
public class UttakGrunnlagTjeneste {

    private static final Period INTERVALL_SAMME_BARN = Period.ofWeeks(6);

    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private PersonopplysningRepository personopplysningRepository;
    private SøknadRepository søknadRepository;
    private PleiepengerRepository pleiepengerRepository;
    private UføretrygdRepository uføretrygdRepository;
    private NesteSakRepository nesteSakRepository;

    @Inject
    public UttakGrunnlagTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                 BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider,
                                 RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                 FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.fagsakRelasjonRepository = behandlingRepositoryProvider.getFagsakRelasjonRepository();
        this.behandlingVedtakRepository = behandlingRepositoryProvider.getBehandlingVedtakRepository();
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.personopplysningRepository = behandlingRepositoryProvider.getPersonopplysningRepository();
        this.søknadRepository = grunnlagRepositoryProvider.getSøknadRepository();
        this.pleiepengerRepository = grunnlagRepositoryProvider.getPleiepengerRepository();
        this.uføretrygdRepository = grunnlagRepositoryProvider.getUføretrygdRepository();
        this.nesteSakRepository = grunnlagRepositoryProvider.getNesteSakRepository();
    }

    UttakGrunnlagTjeneste() {
        //CDI
    }

    public ForeldrepengerGrunnlag grunnlag(BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        var saksnummer = ref.saksnummer();

        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonHvisEksisterer(saksnummer);

        var fhaOpt = familieHendelseTjeneste.finnAggregat(behandlingId);
        if (fhaOpt.isEmpty()) {
            return null;
        }
        var familiehendelser = familieHendelser(fhaOpt.get());
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var erBerørtBehandling = SpesialBehandling.erBerørtBehandling(behandling) && SpesialBehandling.skalUttakVurderes(behandling);
        var originalBehandling = originalBehandling(behandling);
        var harAnnenForelderES = personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(behandlingId)
            .map(OppgittAnnenPartEntitet::getAktørId)
            .filter(a -> annenpartHarInnvilgetES(familiehendelser, a))
            .isPresent();

        var grunnlag = new ForeldrepengerGrunnlag()
            .medErBerørtBehandling(erBerørtBehandling)
            .medFamilieHendelser(familiehendelser)
            .medOppgittAnnenForelderHarEngangsstønadForSammeBarn(harAnnenForelderES)
            .medOriginalBehandling(originalBehandling.orElse(null))
            .medPleiepengerGrunnlag(pleiepengerGrunnlag(ref).orElse(null))
            .medOppdagetPleiepengerOverlappendeUtbetaling(behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_VEDTAK_PLEIEPENGER))
            .medUføretrygdGrunnlag(uføretrygdGrunnlag(ref).orElse(null))
            .medNesteSakGrunnlag(nesteSakGrunnlag(ref).orElse(null));
        if (fagsakRelasjon.isPresent()) {
            var annenpart = annenpart(fagsakRelasjon.get(), behandling);
            grunnlag = grunnlag.medAnnenpart(annenpart.orElse(null));
        }
        return grunnlag;
    }

    private Optional<PleiepengerGrunnlagEntitet> pleiepengerGrunnlag(BehandlingReferanse ref) {
        return pleiepengerRepository.hentGrunnlag(ref.behandlingId());
    }

    private Optional<UføretrygdGrunnlagEntitet> uføretrygdGrunnlag(BehandlingReferanse ref) {
        return uføretrygdRepository.hentGrunnlag(ref.behandlingId());
    }

    private Optional<NesteSakGrunnlagEntitet> nesteSakGrunnlag(BehandlingReferanse ref) {
        return nesteSakRepository.hentGrunnlag(ref.behandlingId());
    }

    private Optional<Annenpart> annenpart(FagsakRelasjon fagsakRelasjon, Behandling behandling) {
        var relatertFagsak = fagsakRelasjon.getRelatertFagsak(behandling.getFagsak());
        if (relatertFagsak.isPresent()) {
            Optional<Behandling> annenPartBehandling;
            var harVedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId()).isPresent();
            if (harVedtak) {
                annenPartBehandling = relatertBehandlingTjeneste.hentAnnenPartsGjeldendeBehandlingPåVedtakstidspunkt(behandling);
            } else {
                annenPartBehandling = relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(behandling.getFagsak().getSaksnummer());
            }
            if (annenPartBehandling.isPresent()) {
                var opprettetTidspunkt = Optional.ofNullable(søknadRepository.hentSøknad(annenPartBehandling.get().getId()))
                    .map(SøknadEntitet::getOpprettetTidspunkt).orElse(annenPartBehandling.get().getOpprettetTidspunkt());
                return Optional.of(
                    new Annenpart(annenPartBehandling.get().getId(), opprettetTidspunkt));
            }
        }
        return Optional.empty();
    }

    private Optional<OriginalBehandling> originalBehandling(Behandling behandling) {
        return behandling.getOriginalBehandlingId()
            .map(obid -> new OriginalBehandling(obid, familieHendelser(familieHendelseTjeneste.hentAggregat(obid))));
    }

    private FamilieHendelser familieHendelser(FamilieHendelseGrunnlagEntitet familieHendelseAggregat) {
        var gjelderFødsel = FamilieHendelseType.gjelderFødsel(familieHendelseAggregat.getGjeldendeVersjon().getType());
        var søknadFamilieHendelse = map(familieHendelseAggregat.getSøknadVersjon(), gjelderFødsel);
        var bekreftetFamilieHendelse = familieHendelseAggregat.getBekreftetVersjon().map(bfh -> map(bfh, gjelderFødsel)).orElse(null);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse)
            .medBekreftetHendelse(bekreftetFamilieHendelse);
        var overstyrtVersjon = familieHendelseAggregat.getOverstyrtVersjon();
        //        Må sjekke om saksbehandler har dokumentert pga at overstyrt versjon av familiehendelse ikke inneholder fødselsdato hvis saksbehandler velger at fødsel ikke er dokumentert
        //        Dette skaper problemer i en revurdering etter avslag pga fødselsvilkåret, der vi trenger gjeldende fødselsdato fra forrige behandling
        //        Se TFP-1880
        if (overstyrtVersjon.isPresent() && (!gjelderFødsel || saksbehandlerHarValgAtFødselErDokumentert(
            overstyrtVersjon.get()))) {
            var overstyrtFamilieHendelse = map(overstyrtVersjon.get(), gjelderFødsel);
            familieHendelser = familieHendelser.medOverstyrtHendelse(overstyrtFamilieHendelse);
        }
        return familieHendelser;
    }

    private boolean saksbehandlerHarValgAtFødselErDokumentert(FamilieHendelseEntitet familieHendelseAggregat) {
        // Antall barn settes til 0 hvis saksbehandler saksbehandler velger ikke
        // dokumentert fødsel
        return familieHendelseAggregat.getAntallBarn() > 0;
    }

    private FamilieHendelse map(FamilieHendelseEntitet familieHendelseEntitet, boolean gjelderFødsel) {
        var barna = familieHendelseEntitet.getBarna()
            .stream()
            .map(b -> new Barn(b.getDødsdato().orElse(null)))
            .toList();
        var antallBarn = familieHendelseEntitet.getAntallBarn();
        if (gjelderFødsel) {
            var termindato = familieHendelseEntitet.getTerminbekreftelse()
                .map(TerminbekreftelseEntitet::getTermindato)
                .orElse(null);
            var fødselsdato = familieHendelseEntitet.getFødselsdato().orElse(null);
            return FamilieHendelse.forFødsel(termindato, fødselsdato, barna, antallBarn);
        } else {
            var adopsjon = familieHendelseEntitet.getAdopsjon()
                .orElseThrow(() -> new IllegalStateException("Forventer adopsjon familiehendelse ved adopsjon/omsorgsovertakelse"));
            var omsorgsovertakelse = adopsjon.getOmsorgsovertakelseDato();
            var ankomstNorge = adopsjon.getAnkomstNorgeDato();
            var erStebarnsadopsjon = adopsjon.isStebarnsadopsjon();
            return FamilieHendelse.forAdopsjonOmsorgsovertakelse(omsorgsovertakelse, barna, antallBarn, ankomstNorge, erStebarnsadopsjon);
        }
    }

    private boolean annenpartHarInnvilgetES(FamilieHendelser familieHendelser, AktørId annenpartAktørId) {
        return relatertBehandlingTjeneste.hentAnnenPartsInnvilgeteFagsakerMedYtelseType(annenpartAktørId, FagsakYtelseType.ENGANGSTØNAD)
            .stream()
            .flatMap(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()).stream())
            .anyMatch(b -> matcherFamiliehendelseMedSak(familieHendelser, b));
    }

    private boolean matcherFamiliehendelseMedSak(FamilieHendelser familieHendelser, Behandling behandling) {
        var egenHendelsedato = familieHendelser.getGjeldendeFamilieHendelse().getFamilieHendelseDato();
        var egetIntervall = new LocalDateInterval(egenHendelsedato.minus(INTERVALL_SAMME_BARN), egenHendelsedato.plus(INTERVALL_SAMME_BARN));
        var annenpartIntervall = familieHendelseTjeneste.finnAggregat(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt)
            .map(d -> new LocalDateInterval(d.minus(INTERVALL_SAMME_BARN), d.plus(INTERVALL_SAMME_BARN)));

        return annenpartIntervall.filter(i -> i.overlaps(egetIntervall)).isPresent();
    }
}
