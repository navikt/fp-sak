package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;

import java.util.Optional;

@ApplicationScoped
public class UttakGrunnlagTjeneste {

    private FamilieHendelseRepository familieHendelseRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private NesteSakRepository nesteSakRepository;

    @Inject
    public UttakGrunnlagTjeneste(FamilieHendelseRepository familieHendelseRepository,
                                 SvangerskapspengerRepository svangerskapspengerRepository,
                                 NesteSakRepository nesteSakRepository) {
        this.familieHendelseRepository = familieHendelseRepository;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.nesteSakRepository = nesteSakRepository;
    }

    UttakGrunnlagTjeneste() {
        //CDI
    }

    public SvangerskapspengerGrunnlag grunnlag(BehandlingReferanse ref) {
        var familieHendelse = familieHendelse(ref);
        return familieHendelse.map(hendelse -> new SvangerskapspengerGrunnlag().medFamilieHendelse(hendelse)
            .medNesteSakEntitet(nesteSakGrunnlag(ref).orElse(null))
            .medSvpGrunnlagEntitet(svpGrunnEntitet(ref).orElse(null))).orElse(null);
    }

    private Optional<SvpGrunnlagEntitet> svpGrunnEntitet(BehandlingReferanse ref) {
        return svangerskapspengerRepository.hentGrunnlag(ref.behandlingId());
    }

    private Optional<NesteSakGrunnlagEntitet> nesteSakGrunnlag(BehandlingReferanse ref) {
        return nesteSakRepository.hentGrunnlag(ref.behandlingId());
    }

    private Optional<FamilieHendelse> familieHendelse(BehandlingReferanse ref) {
        var gjeldendeFamiliehendelseOpt = familieHendelseRepository.hentAggregatHvisEksisterer(ref.behandlingId())
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon);
        if (gjeldendeFamiliehendelseOpt.isEmpty()) {
            return Optional.empty();
        }
        var gjeldendeFamiliehendelse = gjeldendeFamiliehendelseOpt.get();
        var terminbekreftelse = gjeldendeFamiliehendelse.getTerminbekreftelse();
        if (terminbekreftelse.isEmpty()) {
            throw new IllegalStateException("Det skal alltid være termindato på svangerskapspenger søknad.");
        }

        var termindato = terminbekreftelse.get().getTermindato();
        var fødselsdatoOptional = gjeldendeFamiliehendelse.getFødselsdato();
        var barna = gjeldendeFamiliehendelse.getBarna().stream().map(ib -> new Barn(ib.getDødsdato().orElse(null))).toList();
        return Optional.of(FamilieHendelse.forFødsel(termindato, fødselsdatoOptional.orElse(null), barna, gjeldendeFamiliehendelse.getAntallBarn()));
    }
}
