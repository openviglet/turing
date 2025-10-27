import { DraggableTable, type Facet } from "@/components/draggable-table";
import { SubPageHeader } from "@/components/sub.page.header";
import { IconReorder } from "@tabler/icons-react";

export default function SNSiteFacetOrderingPage() {
  const initialFacets: Facet[] = [
    { position: 1, facetName: 'Tipo de conteúdo', fieldName: 'tipos-de-conteudo', id: 'tipos-de-conteudo' },
    { position: 2, facetName: 'Vínculo', fieldName: 'vinculo', id: 'vinculo' },
    { position: 3, facetName: 'Modalidade', fieldName: 'formato-de-aula', id: 'formato-de-aula' },
    { position: 4, facetName: 'Tipo de Curso', fieldName: 'programas', id: 'programas' },
    { position: 5, facetName: 'Área de conhecimento', fieldName: 'area-de-conhecimento', id: 'area-de-conhecimento' },
    { position: 6, facetName: 'Vertente de Carreira', fieldName: 'vertente-de-carreira', id: 'vertente-de-carreira' },
    { position: 7, facetName: 'Unidades Acadêmicas', fieldName: 'temas', id: 'temas' },
    { position: 8, facetName: 'Centros de Conhecimento', fieldName: 'centro-de-conhecimento', id: 'centro-de-conhecimento' },
    { position: 9, facetName: 'Programa', fieldName: 'formato-de-programa', id: 'formato-de-programa' },
  ];
  return (
    <><SubPageHeader icon={IconReorder} title="Facet Ordering" description="Order the facets of the search." />
      <DraggableTable initialData={initialFacets} /></>
  )
}
