import { Edge, Node, ClusterNode } from '@swimlane/ngx-graph';

export const nodes: Node[] = [
  {
    id: 'A',
    label: 'A'
  }, {
    id: 'B',
    label: 'B'
  }, {
    id: 'c1',
    label: 'C1'
  }, {
    id: 'c2',
    label: 'C2'
  },
  {
    id: 'D',
    label: 'D'
  },
  {
    id: 'E',
    label: 'E'
  }
,
{
  id: 'F',
  label: 'F'
},
{
  id: 'G',
  label: 'G'
},
{
  id: 'H',
  label: 'H'
},
{
  id: 'I',
  label: 'I'
},
{
  id: 'J',
  label: 'J'
}
];

export const links: Edge[] = [
  {
    id: 'a',
    source: 'A',
    target: 'B',
    label: 'is parent of'
  }, {
    id: 'b',
    source: 'A',
    target: 'c1',
    label: ' label1'
  }, {
    id: 'c',
    source: 'A',
    target: 'c1',
    label: 'custom '
  }, {
    id: 'd',
    source: 'A',
    target: 'c2',
    label: ' label'
  },{
    id: 'f',
    source: 'B',
    target: 'c2',
    label: ' label'
  },
  {
    id: 'f1',
    source: 'D',
    target: 'E',
    label: ' label'
  },
  {
    id: 'f2',
    source: 'B',
    target: 'F',
    label: ' label'
  },
  {
    id: 'f3',
    source: 'E',
    target: 'F',
    label: ' label'
  },
  {
    id: 'f4',
    source: 'G',
    target: 'A',
    label: ' label'
  },
  {
    id: 'f5',
    source: 'H',
    target: 'F',
    label: ' label'
  },
  {
    id: 'f6',
    source: 'G',
    target: 'I',
    label: ' label'
  },
  {
    id: 'f7',
    source: 'B',
    target: 'J',
    label: ' label'
  }
  ,
  {
    id: 'f8',
    source: 'B',
    target: 'J',
    label: ' label'
  },
  {
    id: 'f9',
    source: 'B',
    target: 'J',
    label: ' label'
  },
  {
    id: 'f0',
    source: 'G',
    target: 'J',
    label: ' label'
  }
];