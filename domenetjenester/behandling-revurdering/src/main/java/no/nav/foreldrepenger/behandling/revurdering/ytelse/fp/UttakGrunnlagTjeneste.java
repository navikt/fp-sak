package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.TapendeBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.YtelsesesspesifiktGrunnlagTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.OriginalBehandling;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class UttakGrunnlagTjeneste implements YtelsesesspesifiktGrunnlagTjeneste {

    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private TapendeBehandlingTjeneste tapendeBehandlingTjeneste;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    public UttakGrunnlagTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
            TapendeBehandlingTjeneste tapendeBehandlingTjeneste,
            RelatertBehandlingTjeneste relatertBehandlingTjeneste,
            FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.fagsakRelasjonRepository = behandlingRepositoryProvider.getFagsakRelasjonRepository();
        this.behandlingVedtakRepository = behandlingRepositoryProvider.getBehandlingVedtakRepository();
        this.tapendeBehandlingTjeneste = tapendeBehandlingTjeneste;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    UttakGrunnlagTjeneste() {
        // for CDI proxy
    }

    @Override
    public Optional<ForeldrepengerGrunnlag> grunnlag(BehandlingReferanse ref) {
        var behandlingId = ref.getBehandlingId();
        var saksnummer = ref.getSaksnummer();

        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonHvisEksisterer(saksnummer);

        var fhaOpt = familieHendelseTjeneste.finnAggregat(behandlingId);
        if (fhaOpt.isEmpty()) {
            return Optional.empty();
        }
        var familiehendelser = familieHendelser(fhaOpt.get());
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var erTapendeBehandling = tapendeBehandlingTjeneste.erTapendeBehandling(behandling);
        var originalBehandling = originalBehandling(behandling);
        var grunnlag = new ForeldrepengerGrunnlag()
                .medErTapendeBehandling(erTapendeBehandling)
                .medFamilieHendelser(familiehendelser)
                .medOriginalBehandling(originalBehandling.orElse(null));
        if (fagsakRelasjon.isPresent()) {
            var annenpart = annenpart(fagsakRelasjon.get(), familiehendelser, behandling);
            grunnlag = grunnlag.medAnnenpart(annenpart.orElse(null));

        }
        return Optional.of(grunnlag);
    }

    private Optional<Annenpart> annenpart(FagsakRelasjon fagsakRelasjon, FamilieHendelser familiehendelser, Behandling behandling) {
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
                return Optional.of(new Annenpart(annenpartHarInnvilgetES(familiehendelser, relatertFagsak.get().getAktørId()),
                        annenPartBehandling.get().getId()));
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
        var bekreftetFamilieHendelse = familieHendelseAggregat.getBekreftetVersjon().isPresent()
                ? map(familieHendelseAggregat.getBekreftetVersjon().get(), gjelderFødsel)
                : null;
        var familieHendelser = new FamilieHendelser()
                .medSøknadHendelse(søknadFamilieHendelse)
                .medBekreftetHendelse(bekreftetFamilieHendelse);
        var overstyrtVersjon = familieHendelseAggregat.getOverstyrtVersjon();
//        Må sjekke om saksbehandler har dokumentert pga at overstyrt versjon av familiehendelse ikke inneholder fødselsdato hvis saksbehandler velger at fødsel ikke er dokumentert
//        Dette skaper problemer i en revurdering etter avslag pga fødselsvilkåret, der vi trenger gjeldende fødselsdato fra forrige behandling
//        Se TFP-1880
        if (overstyrtVersjon.isPresent() && (!gjelderFødsel || saksbehandlerHarValgAtFødselErDokumentert(overstyrtVersjon.get()))) {
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
        var barna = familieHendelseEntitet.getBarna().stream().map(b -> new Barn(b.getDødsdato().orElse(null))).collect(Collectors.toList());
        var antallBarn = familieHendelseEntitet.getAntallBarn();
        if (gjelderFødsel) {
            var termindato = familieHendelseEntitet.getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null);
            var fødselsdato = familieHendelseEntitet.getFødselsdato().orElse(null);
            return FamilieHendelse.forFødsel(termindato, fødselsdato, barna, antallBarn);
        }
        if (familieHendelseEntitet.getAdopsjon().isEmpty()) {
            throw new IllegalStateException("Forventer adopsjon familiehendelse ved adopsjon/omsorgsovertakelse");
        }
        var adopsjon = familieHendelseEntitet.getAdopsjon().get();
        var omsorgsovertakelse = adopsjon.getOmsorgsovertakelseDato();
        var ankomstNorge = adopsjon.getAnkomstNorgeDato();
        boolean erStebarnsadopsjon = erDetStebarnsadopsjon(adopsjon);
        return FamilieHendelse.forAdopsjonOmsorgsovertakelse(omsorgsovertakelse, barna, antallBarn, ankomstNorge, erStebarnsadopsjon);
    }

    private boolean erDetStebarnsadopsjon(no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet adopsjon) {
        if (adopsjon.getErEktefellesBarn() != null) {
            return adopsjon.getErEktefellesBarn();
        }
        return false;
    }

    private boolean annenpartHarInnvilgetES(FamilieHendelser familieHendelser, AktørId annenpartAktørId) {
        return relatertBehandlingTjeneste.hentAnnenPartsInnvilgeteFagsakerMedYtelseType(annenpartAktørId, FagsakYtelseType.ENGANGSTØNAD).stream()
                .flatMap(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()).stream())
                .anyMatch(b -> familieHendelseTjeneste
                        .harBehandlingFamilieHendelseDato(familieHendelser.getGjeldendeFamilieHendelse().getFamilieHendelseDato(), b.getId()));
    }
}
