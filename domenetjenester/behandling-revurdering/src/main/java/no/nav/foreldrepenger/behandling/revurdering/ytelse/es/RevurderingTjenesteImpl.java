package no.nav.foreldrepenger.behandling.revurdering.ytelse.es;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingFeil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class RevurderingTjenesteImpl implements RevurderingTjeneste {
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;
    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
    private SøknadRepository søknadRepository;
    private RevurderingTjenesteFelles revurderingTjenesteFelles;
    private RevurderingEndring revurderingEndring;
    private VergeRepository vergeRepository;

    public RevurderingTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjenesteImpl(BehandlingRepository behandlingRepository,
                                   BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider,
                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                   @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD) RevurderingEndring revurderingEndring,
                                   RevurderingTjenesteFelles revurderingTjenesteFelles,
                                   VergeRepository vergeRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.familieHendelseRepository = grunnlagRepositoryProvider.getFamilieHendelseRepository();
        this.personopplysningRepository = grunnlagRepositoryProvider.getPersonopplysningRepository();
        this.medlemskapRepository = grunnlagRepositoryProvider.getMedlemskapRepository();
        this.søknadRepository = grunnlagRepositoryProvider.getSøknadRepository();
        this.revurderingEndring = revurderingEndring;
        this.revurderingTjenesteFelles = revurderingTjenesteFelles;
        this.vergeRepository = vergeRepository;
    }

    @Override
    public Behandling opprettManuellRevurdering(Fagsak fagsak,
                                                BehandlingÅrsakType revurderingsÅrsak,
                                                OrganisasjonsEnhet enhet,
                                                String opprettetAv) {
        return opprettRevurdering(fagsak, revurderingsÅrsak, true, enhet, opprettetAv);
    }

    @Override
    public Behandling opprettAutomatiskRevurdering(Fagsak fagsak,
                                                   BehandlingÅrsakType revurderingsÅrsak,
                                                   OrganisasjonsEnhet enhet) {
        return opprettRevurdering(fagsak, revurderingsÅrsak, false, enhet, null);
    }

    private Behandling opprettRevurdering(Fagsak fagsak,
                                          BehandlingÅrsakType revurderingsÅrsak,
                                          boolean manueltOpprettet,
                                          OrganisasjonsEnhet enhet, String opprettetAv) {
        var origBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .orElseThrow(() -> RevurderingFeil.tjenesteFinnerIkkeBehandlingForRevurdering(fagsak.getId()));

        // lås original behandling først slik at ingen andre forsøker på samme
        behandlingskontrollTjeneste.initBehandlingskontroll(origBehandling);

        // deretter opprett kontekst for revurdering og opprett
        var revurdering = revurderingTjenesteFelles.opprettRevurderingsbehandling(revurderingsÅrsak, origBehandling, manueltOpprettet, enhet, opprettetAv);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering);
        behandlingskontrollTjeneste.opprettBehandling(kontekst, revurdering);
        revurderingTjenesteFelles.opprettHistorikkInnslagForNyRevurdering(revurdering, revurderingsÅrsak, manueltOpprettet);

        // Kopier søknadsdata
        søknadRepository.hentSøknadHvisEksisterer(origBehandling.getId())
            .ifPresent(s -> søknadRepository.lagreOgFlush(revurdering, s));
        kopierAlleGrunnlagFraTidligereBehandling(origBehandling, revurdering);
        return revurdering;
    }

    @Override
    public void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny) {
        var orginalBehandlingId = original.getId();
        var nyBehandlingId = ny.getId();
        if (BehandlingType.REVURDERING.equals(ny.getType())) {
            familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(orginalBehandlingId,
                nyBehandlingId);
            if (ny.harBehandlingÅrsak(
                BehandlingÅrsakType.RE_HENDELSE_FØDSEL)) { // Unngå manuell re-evaluering i tilfelle "automatisk" revurdering
                personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
                medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
                vergeRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
            } else {
                personopplysningRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(orginalBehandlingId,
                    nyBehandlingId);
                medlemskapRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(orginalBehandlingId,
                    nyBehandlingId);
            }
        } else {
            familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
            personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
            medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
            vergeRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
        }
    }

    @Override
    public Boolean kanRevurderingOpprettes(Fagsak fagsak) {
        return revurderingTjenesteFelles.kanRevurderingOpprettes(fagsak);
    }

    @Override
    public boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        return revurderingEndring.erRevurderingMedUendretUtfall(behandling);
    }

}
